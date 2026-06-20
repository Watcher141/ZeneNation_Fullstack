// src/pages/user/CheckoutPage.jsx
import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useCart } from '../../context/CartContext';
import { addressApi, orderApi, couponApi, rewardApi, shippingApi } from '../../api/apiCollections';
import toast from 'react-hot-toast';
import Loader from '../../components/common/Loader';
import {
  MdLocationOn, MdPayment, MdShoppingCart, MdLocalShipping,
  MdCreditCard, MdCheckCircle, MdArrowBack, MdLocalOffer,
  MdAdd, MdClose, MdCardGiftcard,
} from 'react-icons/md';
import { BsCash } from 'react-icons/bs';
import { RiSecurePaymentLine } from 'react-icons/ri';
import './CheckoutPage.css';

const CheckoutPage = () => {
  const { cart, fetchCart } = useCart();
  const navigate = useNavigate();

  const [addresses, setAddresses] = useState([]);
  const [selectedAddress, setSelectedAddress] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('COD');
  const [couponCode, setCouponCode] = useState('');
  const [couponData, setCouponData] = useState(null);
  const [couponLoading, setCouponLoading] = useState(false);
  const [placing, setPlacing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [showAddressForm, setShowAddressForm] = useState(false);
  const [addressForm, setAddressForm] = useState({
    name: '', phoneNumber: '', addressLine1: '', addressLine2: '',
    city: '', state: '', pincode: '', isDefault: false,
  });
  const [savingAddress, setSavingAddress] = useState(false);
  const [rewardsBalance, setRewardsBalance] = useState(0);
  const [redeemPoints, setRedeemPoints] = useState(0);
  const [maxRedeemable, setMaxRedeemable] = useState(0);
  const [useRewards, setUseRewards] = useState(false);
  const [preorderPaymentType, setPreorderPaymentType] = useState('FULL');
  const [shippingConfig, setShippingConfig] = useState(null);

  // Must be defined before derived calculations
  const items = cart?.items || [];
  const hasPreorderItems = items.some(item => item.isPreorder);

  const totalWeightGrams = items.reduce((sum, item) => sum + (item.weightGrams || 0) * item.quantity, 0);

  const calculateDeliveryCharge = (weight, slabs) => {
    if (!slabs || slabs.length === 0) return 0;
    const w = Math.max(weight, 1);
    for (const slab of slabs) {
      if (w >= slab.minWeightGrams && w <= slab.maxWeightGrams) {
        return Number(slab.charge);
      }
    }
    const highest = slabs[slabs.length - 1];
    return Number(highest.charge);
  };

  const calculateCodCharge = (sub, slabs) => {
    if (!slabs || slabs.length === 0) return 0;
    for (const slab of slabs) {
      if (sub >= Number(slab.minOrderAmount) && sub <= Number(slab.maxOrderAmount)) {
        return Number(slab.extraCharge);
      }
    }
    const highest = slabs[slabs.length - 1];
    return Number(highest.extraCharge);
  };

  const subtotal = Number(cart?.subtotal || 0);
  const deliveryCharge = shippingConfig
    ? calculateDeliveryCharge(totalWeightGrams, shippingConfig.deliverySlabs)
    : 0;
  const codCharge = paymentMethod === 'COD' && shippingConfig
    ? calculateCodCharge(subtotal, shippingConfig.codSlabs)
    : 0;

  const discountAmount = couponData ? Number(couponData.discountAmount) : 0;
  // Reward: max redeemable = 60% of balance, only if subtotal >= ₹399
  const isRewardEligible = subtotal >= 399;
  const rewardsDiscount = useRewards ? Math.floor(redeemPoints / 2) : 0;  // 2 pts = ₹1
  const total = Math.max(0, subtotal + deliveryCharge + codCharge - discountAmount - rewardsDiscount);
  const codLimit = 10000;

  // Preorder amount calculations
  const payNowAmount = hasPreorderItems && preorderPaymentType === 'HALF'
    ? Math.ceil(total / 2)
    : total;

  useEffect(() => {
    rewardApi.getWallet()
      .then(res => setRewardsBalance(res.data.data?.balance || 0))
      .catch(() => {});

    shippingApi.getConfig()
      .then(res => setShippingConfig(res.data.data))
      .catch(err => console.error('Failed to load shipping config', err));
  }, []);

  useEffect(() => {
    addressApi.getAll()
      .then(res => {
        const list = res.data.data || [];
        setAddresses(list);
        const def = list.find(a => a.isDefault) || list[0];
        if (def) setSelectedAddress(def.id);
        if (list.length === 0) setShowAddressForm(true);
      })
      .catch(() => toast.error('Failed to load addresses'))
      .finally(() => setLoading(false));
  }, []);

  const handleApplyCoupon = async () => {
    if (!couponCode.trim()) return;
    setCouponLoading(true);
    try {
      const res = await couponApi.validate({ code: couponCode }, subtotal + deliveryCharge);
      setCouponData(res.data.data);
      toast.success(res.data.data.message);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Invalid coupon');
      setCouponData(null);
    } finally { setCouponLoading(false); }
  };

  const handleRemoveCoupon = () => { setCouponCode(''); setCouponData(null); };

  const handleSaveAddress = async (e) => {
    e.preventDefault();
    setSavingAddress(true);
    try {
      const res = await addressApi.add(addressForm);
      const newAddr = res.data.data;
      setAddresses(prev => [...prev, newAddr]);
      setSelectedAddress(newAddr.id);
      setShowAddressForm(false);
      setAddressForm({ name: '', phoneNumber: '', addressLine1: '', addressLine2: '', city: '', state: '', pincode: '', isDefault: false });
      toast.success('Address saved!');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save address');
    } finally { setSavingAddress(false); }
  };

  const handlePlaceOrder = async () => {
    if (!selectedAddress) { toast.error('Please select a delivery address'); return; }
    if (!items.length) { toast.error('Your cart is empty'); return; }
    if (paymentMethod === 'COD' && total > codLimit) {
      toast.error(`COD not available for orders above Rs.${codLimit.toLocaleString('en-IN')}`);
      return;
    }
    setPlacing(true);
    try {
      const payload = {
        addressId: selectedAddress,
        paymentMethod,
        couponCode: couponData ? couponCode : null,
        redeemPoints: useRewards ? redeemPoints : 0,
        preorderPaymentType: hasPreorderItems ? preorderPaymentType : null,
      };
      const res = await orderApi.placeOrder(payload);
      const order = res.data.data;
      if (paymentMethod === 'ONLINE' && order.razorpayOrderId) {
        openRazorpay(order);
      } else {
        await fetchCart();
        toast.success('Order placed successfully!');
        navigate('/orders');
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to place order');
    } finally { setPlacing(false); }
  };

  const openRazorpay = (order) => {
    const options = {
      key: import.meta.env.VITE_RAZORPAY_KEY_ID || 'rzp_test_placeholder',
      amount: Math.round(payNowAmount * 100),
      currency: 'INR',
      name: 'Zenenation',
      description: `Order ${order.orderNumber}`,
      order_id: order.razorpayOrderId,
      handler: async (response) => {
        try {
          const api = await import('../../api/axiosInstance');
          await api.default.post('/api/v1/payments/verify', {
            orderId: order.id,
            razorpayOrderId: response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature,
          });
          await fetchCart();
          toast.success('Payment successful! Order confirmed.');
          navigate('/orders');
        } catch {
          toast.error('Payment verification failed. Contact support.');
          navigate('/orders');
        }
      },
      prefill: {
        name: addresses.find(a => a.id === selectedAddress)?.name || '',
        contact: addresses.find(a => a.id === selectedAddress)?.phoneNumber || '',
      },
      theme: { color: '#e94560' },
      modal: { ondismiss: () => { toast.error('Payment cancelled.'); navigate('/orders'); } }
    };
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => { const rzp = new window.Razorpay(options); rzp.open(); };
    document.body.appendChild(script);
  };

  if (loading) return <Loader fullPage />;

  return (
    <div className="page-wrapper">
      <div className="container checkout-page">
        <h1 className="checkout-title">Checkout</h1>

        <div className="checkout-layout">
          {/* ── LEFT ── */}
          <div className="checkout-left">

            {/* Delivery Address */}
            <div className="checkout-section">
              <h2 className="checkout-section-title">
                <MdLocationOn size={20} color="var(--accent-primary)" /> Delivery Address
              </h2>
              {addresses.length > 0 && (
                <div className="address-list">
                  {addresses.map(addr => (
                    <label key={addr.id} className={`address-card ${selectedAddress === addr.id ? 'selected' : ''}`}>
                      <input type="radio" name="address" value={addr.id}
                        checked={selectedAddress === addr.id}
                        onChange={() => setSelectedAddress(addr.id)} />
                      <div className="address-card-info">
                        <div className="address-card-name">
                          {addr.name}
                          {addr.isDefault && <span className="badge badge-green" style={{ marginLeft: 8, fontSize: 10 }}>Default</span>}
                        </div>
                        <div className="address-card-detail">{addr.phoneNumber}</div>
                        <div className="address-card-detail">
                          {addr.addressLine1}{addr.addressLine2 && `, ${addr.addressLine2}`}, {addr.city}, {addr.state} - {addr.pincode}
                        </div>
                      </div>
                    </label>
                  ))}
                </div>
              )}
              <button className="btn btn-ghost btn-sm"
                style={{ marginTop: 'var(--space-3)', display: 'flex', alignItems: 'center', gap: 6 }}
                onClick={() => setShowAddressForm(!showAddressForm)}>
                {showAddressForm ? <><MdClose size={16} /> Cancel</> : <><MdAdd size={16} /> Add New Address</>}
              </button>
              {showAddressForm && (
                <form onSubmit={handleSaveAddress} className="address-form">
                  <div className="address-form-grid">
                    <div className="form-group">
                      <label className="form-label">Full Name *</label>
                      <input className="form-input" value={addressForm.name}
                        onChange={e => setAddressForm({ ...addressForm, name: e.target.value })} required placeholder="Recipient name" />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Phone Number *</label>
                      <input className="form-input" value={addressForm.phoneNumber}
                        onChange={e => setAddressForm({ ...addressForm, phoneNumber: e.target.value })} required placeholder="10-digit mobile" />
                    </div>
                    <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                      <label className="form-label">Address Line 1 *</label>
                      <input className="form-input" value={addressForm.addressLine1}
                        onChange={e => setAddressForm({ ...addressForm, addressLine1: e.target.value })} required placeholder="House/Flat no, Building" />
                    </div>
                    <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                      <label className="form-label">Address Line 2</label>
                      <input className="form-input" value={addressForm.addressLine2}
                        onChange={e => setAddressForm({ ...addressForm, addressLine2: e.target.value })} placeholder="Street, Locality (optional)" />
                    </div>
                    <div className="form-group">
                      <label className="form-label">City *</label>
                      <input className="form-input" value={addressForm.city}
                        onChange={e => setAddressForm({ ...addressForm, city: e.target.value })} required placeholder="City" />
                    </div>
                    <div className="form-group">
                      <label className="form-label">State *</label>
                      <input className="form-input" value={addressForm.state}
                        onChange={e => setAddressForm({ ...addressForm, state: e.target.value })} required placeholder="State" />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Pincode *</label>
                      <input className="form-input" value={addressForm.pincode}
                        onChange={e => setAddressForm({ ...addressForm, pincode: e.target.value })} required placeholder="6-digit pincode" />
                    </div>
                    <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <input type="checkbox" id="isDefault" checked={addressForm.isDefault}
                        onChange={e => setAddressForm({ ...addressForm, isDefault: e.target.checked })} />
                      <label htmlFor="isDefault" className="form-label" style={{ margin: 0, cursor: 'pointer' }}>Set as default</label>
                    </div>
                  </div>
                  <button type="submit" className="btn btn-primary btn-sm" disabled={savingAddress} style={{ marginTop: 'var(--space-4)' }}>
                    {savingAddress ? 'Saving...' : 'Save Address'}
                  </button>
                </form>
              )}
            </div>

            {/* Payment Method */}
            <div className="checkout-section">
              <h2 className="checkout-section-title">
                <MdPayment size={20} color="var(--accent-primary)" /> Payment Method
              </h2>
              <div className="payment-options">
                <label className={`payment-option ${paymentMethod === 'COD' ? 'selected' : ''}`}>
                  <input type="radio" name="payment" value="COD"
                    checked={paymentMethod === 'COD'} onChange={() => setPaymentMethod('COD')} />
                  <BsCash size={24} color="var(--accent-secondary)" />
                  <div>
                    <div style={{ fontWeight: 600 }}>Cash on Delivery</div>
                    <div className="text-xs text-muted">Pay when your order arrives (max Rs.{codLimit.toLocaleString('en-IN')})</div>
                  </div>
                </label>
                <label className={`payment-option ${paymentMethod === 'ONLINE' ? 'selected' : ''}`}>
                  <input type="radio" name="payment" value="ONLINE"
                    checked={paymentMethod === 'ONLINE'} onChange={() => setPaymentMethod('ONLINE')} />
                  <RiSecurePaymentLine size={24} color="var(--accent-blue)" />
                  <div>
                    <div style={{ fontWeight: 600 }}>Online Payment</div>
                    <div className="text-xs text-muted">UPI, Cards, NetBanking via Razorpay</div>
                  </div>
                </label>
              </div>
            </div>

            {/* Preorder Payment Options */}
            {hasPreorderItems && (
              <div className="checkout-section preorder-payment-section">
                <h2 className="checkout-section-title" style={{ color: 'var(--accent-secondary)' }}>
                  Preorder Payment Option
                </h2>
                <p className="text-muted text-sm" style={{ marginBottom: 'var(--space-4)' }}>
                  Your cart has preorder items. Choose how you would like to pay:
                </p>
                <div className="payment-options">
                  <label className={`payment-option ${preorderPaymentType === 'HALF' ? 'selected' : ''}`}
                    style={preorderPaymentType === 'HALF' ? { borderColor: 'var(--accent-secondary)' } : {}}>
                    <input type="radio" name="preorderPayment" value="HALF"
                      checked={preorderPaymentType === 'HALF'}
                      onChange={() => setPreorderPaymentType('HALF')} />
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 600, color: 'var(--accent-secondary)' }}>Pay 50% Now</div>
                      <div className="text-xs text-muted">
                        Pay Rs.{Math.ceil(total / 2).toLocaleString('en-IN')} now, remaining Rs.{Math.floor(total / 2).toLocaleString('en-IN')} when shipped
                      </div>
                    </div>
                    <span style={{ fontWeight: 700, color: 'var(--accent-secondary)', marginLeft: 'auto', flexShrink: 0 }}>
                      Rs.{Math.ceil(total / 2).toLocaleString('en-IN')}
                    </span>
                  </label>
                  <label className={`payment-option ${preorderPaymentType === 'FULL' ? 'selected' : ''}`}>
                    <input type="radio" name="preorderPayment" value="FULL"
                      checked={preorderPaymentType === 'FULL'}
                      onChange={() => setPreorderPaymentType('FULL')} />
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 600, color: 'var(--accent-green)' }}>Pay 100% Now</div>
                      <div className="text-xs text-muted">Full payment upfront — priority processing</div>
                    </div>
                    <span style={{ fontWeight: 700, color: 'var(--accent-green)', marginLeft: 'auto', flexShrink: 0 }}>
                      Rs.{total.toLocaleString('en-IN')}
                    </span>
                  </label>
                </div>
              </div>
            )}

          </div>

          {/* ── RIGHT — Order Summary ── */}
          <div className="checkout-right">
            <div className="checkout-summary">
              <h2 className="checkout-section-title">
                <MdShoppingCart size={20} color="var(--accent-primary)" /> Order Summary
              </h2>

              <div className="checkout-items">
                {items.map(item => (
                  <div key={item.cartItemId} className="checkout-item">
                    <div className="checkout-item-img">
                      {item.primaryImageUrl
                        ? <img src={item.primaryImageUrl} alt={item.productName} />
                        : <MdShoppingCart size={24} color="var(--text-muted)" />}
                    </div>
                    <div className="checkout-item-info">
                      <p className="checkout-item-name">
                        {item.productName}
                        {item.isPreorder && (
                          <span className="badge badge-red" style={{ marginLeft: 6, fontSize: 9 }}>PREORDER</span>
                        )}
                      </p>
                      <p className="text-xs text-muted">Qty: {item.quantity}</p>
                    </div>
                    <span className="text-gold" style={{ fontWeight: 700, whiteSpace: 'nowrap' }}>
                      Rs.{Number(item.totalPrice).toLocaleString('en-IN')}
                    </span>
                  </div>
                ))}
              </div>

              <div className="divider" />

              {/* Rewards */}
              {rewardsBalance > 0 && (
                <div className="rewards-redeem-section">
                  <div className="rewards-redeem-header">
                    <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: isRewardEligible ? 'pointer' : 'not-allowed', opacity: isRewardEligible ? 1 : 0.5 }}>
                      <input type="checkbox" checked={useRewards} disabled={!isRewardEligible}
                        onChange={e => {
                          setUseRewards(e.target.checked);
                          if (e.target.checked) {
                            // 60% of balance cap
                            const max = Math.floor(rewardsBalance * 0.60);
                            setMaxRedeemable(max);
                            setRedeemPoints(max);
                          } else {
                            setRedeemPoints(0);
                          }
                        }} />
                      <MdCardGiftcard size={18} color="var(--accent-secondary)" />
                      <span style={{ fontWeight: 600, fontSize: 'var(--text-sm)' }}>Use Reward Points</span>
                    </label>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 2 }}>
                      <span className="text-xs text-muted">{rewardsBalance} pts available (₹{Math.floor(rewardsBalance / 2)} value)</span>
                      {!isRewardEligible && (
                        <span className="text-xs" style={{ color: 'var(--accent-primary)' }}>Min ₹399 order to redeem</span>
                      )}
                    </div>
                  </div>
                  {useRewards && isRewardEligible && (
                    <div className="rewards-redeem-input">
                      <input type="range" min={2} max={maxRedeemable} step={2} value={redeemPoints}
                        onChange={e => setRedeemPoints(Number(e.target.value))}
                        style={{ flex: 1, accentColor: 'var(--accent-secondary)' }} />
                      <span className="text-gold" style={{ fontWeight: 700, minWidth: 100, textAlign: 'right', fontSize: 'var(--text-sm)' }}>
                        {redeemPoints} pts = ₹{Math.floor(redeemPoints / 2)} off
                      </span>
                    </div>
                  )}
                </div>
              )}

              {/* Coupon */}
              <div className="coupon-section">
                {couponData ? (
                  <div className="coupon-applied">
                    <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <MdLocalOffer size={16} /> <strong>{couponCode.toUpperCase()}</strong> applied!
                    </span>
                    <button className="btn btn-ghost btn-sm" onClick={handleRemoveCoupon}>
                      <MdClose size={14} /> Remove
                    </button>
                  </div>
                ) : (
                  <div className="coupon-input">
                    <input className="form-input" placeholder="Enter coupon code"
                      value={couponCode} onChange={e => setCouponCode(e.target.value.toUpperCase())}
                      style={{ textTransform: 'uppercase' }} />
                    <button className="btn btn-secondary btn-sm" onClick={handleApplyCoupon} disabled={couponLoading}>
                      {couponLoading ? '...' : 'Apply'}
                    </button>
                  </div>
                )}
              </div>

              <div className="divider" />

              {/* Price Breakdown */}
              <div className="checkout-price-rows">
                <div className="price-row">
                  <span>Subtotal</span>
                  <span>Rs.{subtotal.toLocaleString('en-IN')}</span>
                </div>
                <div className="price-row">
                  <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <MdLocalShipping size={16} /> Delivery
                    {totalWeightGrams > 0 && (
                      <span className="text-xs text-muted" style={{ fontWeight: 'normal' }}>
                        ({totalWeightGrams >= 1000 ? `${(totalWeightGrams / 1000).toFixed(2)} kg` : `${totalWeightGrams}g`})
                      </span>
                    )}
                  </span>
                  <span>{deliveryCharge === 0 ? <span className="text-success">FREE</span> : `Rs.${deliveryCharge}`}</span>
                </div>
                {paymentMethod === 'COD' && codCharge > 0 && (
                  <div className="price-row text-warning">
                    <span>COD Surcharge</span>
                    <span>Rs.{codCharge}</span>
                  </div>
                )}
                {couponData && (
                  <div className="price-row text-success">
                    <span>Discount ({couponCode})</span>
                    <span>-Rs.{discountAmount.toLocaleString('en-IN')}</span>
                  </div>
                )}
                {useRewards && redeemPoints > 0 && (
                  <div className="price-row" style={{ color: 'var(--accent-secondary)' }}>
                    <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                      <MdCardGiftcard size={14} /> Rewards ({redeemPoints} pts)
                    </span>
                    <span>-₹{Math.floor(redeemPoints / 2).toLocaleString('en-IN')}</span>
                  </div>
                )}
                <div className="divider" />
                {hasPreorderItems && preorderPaymentType === 'HALF' ? (
                  <>
                    <div className="price-row">
                      <span>Total</span>
                      <span className="text-gold">Rs.{total.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="price-row price-total" style={{ color: 'var(--accent-secondary)' }}>
                      <span>Pay Now (50%)</span>
                      <span>Rs.{Math.ceil(total / 2).toLocaleString('en-IN')}</span>
                    </div>
                    <div className="price-row" style={{ fontSize: 'var(--text-xs)', color: 'var(--text-muted)' }}>
                      <span>Remaining on shipping</span>
                      <span>Rs.{Math.floor(total / 2).toLocaleString('en-IN')}</span>
                    </div>
                  </>
                ) : (
                  <div className="price-row price-total">
                    <span>Total</span>
                    <span className="text-gold">Rs.{total.toLocaleString('en-IN')}</span>
                  </div>
                )}
              </div>

              <button className="btn btn-primary btn-full btn-lg"
                onClick={handlePlaceOrder}
                disabled={placing || !selectedAddress}
                title={!selectedAddress ? 'Please save your address first' : ''}
                style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
                {placing ? 'Placing order...' :
                  !selectedAddress ? 'Save address first' :
                  hasPreorderItems && preorderPaymentType === 'HALF'
                    ? <><MdCheckCircle size={20} /> Pay Rs.{Math.ceil(total / 2).toLocaleString('en-IN')} Now</>
                    : paymentMethod === 'COD'
                      ? <><MdCheckCircle size={20} /> Place Order</>
                      : <><MdCreditCard size={20} /> Pay Now</>
                }
              </button>

              <Link to="/cart" className="btn btn-ghost btn-full"
                style={{ marginTop: 'var(--space-3)', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
                <MdArrowBack size={18} /> Back to Cart
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CheckoutPage;