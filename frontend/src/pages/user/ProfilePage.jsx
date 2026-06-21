// src/pages/user/ProfilePage.jsx
import { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { userApi, couponApi } from '../../api/apiCollections';
import { addressApi } from '../../api/apiCollections';
import toast from 'react-hot-toast';
import Loader from '../../components/common/Loader';
import {
  MdPerson,
  MdEmail,
  MdPhone,
  MdEdit,
  MdSave,
  MdClose,
  MdLock,
  MdLocationOn,
  MdAdd,
  MdDelete,
  MdStar,
  MdVerified,
  MdLocalOffer,
  MdContentCopy
} from 'react-icons/md';

import { SiGoogle } from 'react-icons/si';


import RewardsWallet from '../../components/user/RewardsWallet';
import './ProfilePage.css';

const ProfilePage = () => {
  const { user, setUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('profile');
  const [welcomeCoupon, setWelcomeCoupon] = useState(null);

  // Edit profile
  const [editingProfile, setEditingProfile] = useState(false);
  const [profileForm, setProfileForm] = useState({ name: '', phoneNumber: '' });
  const [savingProfile, setSavingProfile] = useState(false);

  // Change password
  const [passwordForm, setPasswordForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [savingPassword, setSavingPassword] = useState(false);

  // Address
  const [showAddressForm, setShowAddressForm] = useState(false);
  const [editingAddress, setEditingAddress] = useState(null);
  const [addressForm, setAddressForm] = useState({ name: '', phoneNumber: '', addressLine1: '', addressLine2: '', city: '', state: '', pincode: '', isDefault: false });
  const [savingAddress, setSavingAddress] = useState(false);

  useEffect(() => {
    Promise.all([userApi.getProfile(), addressApi.getAll(), couponApi.getMyWelcomeCoupon()])
      .then(([profileRes, addrRes, couponRes]) => {
        setProfile(profileRes.data.data);
        setAddresses(addrRes.data.data || []);
        setProfileForm({ name: profileRes.data.data.name, phoneNumber: profileRes.data.data.phoneNumber || '' });
        setWelcomeCoupon(couponRes.data.data || null);
      })
      .catch(() => toast.error('Failed to load profile'))
      .finally(() => setLoading(false));
  }, []);

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setSavingProfile(true);
    try {
      const res = await userApi.updateProfile(profileForm);
      setProfile(res.data.data);
      setUser({ ...user, name: res.data.data.name });
      localStorage.setItem('user', JSON.stringify({ ...user, name: res.data.data.name }));
      setEditingProfile(false);
      toast.success('Profile updated!');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update');
    } finally { setSavingProfile(false); }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      toast.error('Passwords do not match'); return;
    }
    setSavingPassword(true);
    try {
      await userApi.changePassword(passwordForm);
      toast.success('Password changed successfully!');
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to change password');
    } finally { setSavingPassword(false); }
  };

  const handleSaveAddress = async (e) => {
    e.preventDefault();
    setSavingAddress(true);
    try {
      if (editingAddress) {
        const res = await addressApi.update(editingAddress.id, addressForm);
        setAddresses(prev => prev.map(a => a.id === editingAddress.id ? res.data.data : a));
        toast.success('Address updated!');
      } else {
        const res = await addressApi.add(addressForm);
        setAddresses(prev => [...prev, res.data.data]);
        toast.success('Address added!');
      }
      setShowAddressForm(false);
      setEditingAddress(null);
      setAddressForm({ name: '', phoneNumber: '', addressLine1: '', addressLine2: '', city: '', state: '', pincode: '', isDefault: false });
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save address');
    } finally { setSavingAddress(false); }
  };

  const handleDeleteAddress = async (id) => {
    if (!window.confirm('Delete this address?')) return;
    try {
      await addressApi.delete(id);
      setAddresses(prev => prev.filter(a => a.id !== id));
      toast.success('Address deleted');
    } catch { toast.error('Failed to delete'); }
  };

  const handleSetDefault = async (id) => {
    try {
      await addressApi.setDefault(id);
      setAddresses(prev => prev.map(a => ({ ...a, isDefault: a.id === id })));
      toast.success('Default address updated');
    } catch { toast.error('Failed to update'); }
  };

  const openEditAddress = (addr) => {
    setEditingAddress(addr);
    setAddressForm({ name: addr.name, phoneNumber: addr.phoneNumber, addressLine1: addr.addressLine1, addressLine2: addr.addressLine2 || '', city: addr.city, state: addr.state, pincode: addr.pincode, isDefault: addr.isDefault });
    setShowAddressForm(true);
  };

  if (loading) return <Loader fullPage />;

  const tabs = [
    { key: 'profile',   label: 'My Profile',  icon: MdPerson     },
    { key: 'addresses', label: 'Addresses',    icon: MdLocationOn },
    { key: 'security',  label: 'Security',     icon: MdLock       },
    { key: 'rewards',   label: 'My Rewards',   icon: MdStar       },
  ];

  return (
    <div className="page-wrapper">
      <div className="container profile-page">
        {/* Header */}
        <div className="profile-header">
          <div className="profile-avatar-wrap">
            {profile?.profileImageUrl
              ? <img src={profile.profileImageUrl} alt={profile.name} className="profile-avatar-img" />
              : <div className="profile-avatar-big">{profile?.name?.charAt(0).toUpperCase()}</div>
            }
          </div>
          <div className="profile-header-info">
            <h1 className="profile-name">{profile?.name}</h1>
            <p className="profile-email">
              <MdEmail size={14} /> {profile?.email}
              {profile?.isEmailVerified && <span className="badge badge-green" style={{ marginLeft: 8 }}><MdVerified size={12} /> Verified</span>}
            </p>
            <div style={{ display: 'flex', gap: 8, marginTop: 8, flexWrap: 'wrap' }}>
              <span className={`badge ${profile?.role === 'ROLE_ADMIN' ? 'badge-red' : 'badge-blue'}`}>
                {profile?.role === 'ROLE_ADMIN' ? 'Admin' : 'Member'}
              </span>
              <span className="badge badge-purple">
                {profile?.provider === 'GOOGLE' ? <><SiGoogle size={12} /> Google</> : 'Email'}
              </span>
              <span className={`badge ${profile?.isActive ? 'badge-green' : 'badge-red'}`}>
                {profile?.isActive ? 'Active' : 'Inactive'}
              </span>
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="profile-tabs">
          {tabs.map(tab => {
            const Icon = tab.icon;
            return (
              <button key={tab.key}
                className={`profile-tab ${activeTab === tab.key ? 'active' : ''}`}
                onClick={() => setActiveTab(tab.key)}>
                <Icon size={18} /> {tab.label}
              </button>
            );
          })}
        </div>

        {/* ── Welcome Coupon Banner ── */}
        {welcomeCoupon && welcomeCoupon.isCurrentlyValid && activeTab === 'profile' && (
          <div className="welcome-coupon-banner">
            <div className="welcome-coupon-icon"><MdLocalOffer size={28} /></div>
            <div className="welcome-coupon-info">
              <div className="welcome-coupon-label"><MdLocalOffer size={16} style={{ marginRight: 6, verticalAlign: 'middle' }} /> Your Welcome Gift</div>
              <div className="welcome-coupon-code"
                onClick={() => { navigator.clipboard.writeText(welcomeCoupon.code); import('react-hot-toast').then(m => m.default.success('Coupon code copied!')); }}
                title="Click to copy">
                {welcomeCoupon.code} <MdContentCopy size={14} style={{ verticalAlign: 'middle', opacity: 0.6 }} />
              </div>
              <div className="welcome-coupon-desc">
                5% off your first order (max ₹{welcomeCoupon.maximumDiscount}) · 
                Expires {new Date(welcomeCoupon.validUntil).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
              </div>
            </div>
            <div className="welcome-coupon-badge">ONE-TIME USE</div>
          </div>
        )}

        {/* ── Profile Tab ── */}
        {activeTab === 'profile' && (
          <div className="profile-section">
            <div className="profile-section-header">
              <h2>Personal Information</h2>
              {!editingProfile && (
                <button className="btn btn-ghost btn-sm" onClick={() => setEditingProfile(true)}>
                  <MdEdit size={16} /> Edit
                </button>
              )}
            </div>

            {editingProfile ? (
              <form onSubmit={handleSaveProfile} className="profile-form">
                <div className="form-group">
                  <label className="form-label">Full Name</label>
                  <input className="form-input" value={profileForm.name}
                    onChange={e => setProfileForm({ ...profileForm, name: e.target.value })} required />
                </div>
                <div className="form-group">
                  <label className="form-label">Phone Number</label>
                  <input className="form-input" value={profileForm.phoneNumber}
                    onChange={e => setProfileForm({ ...profileForm, phoneNumber: e.target.value })}
                    placeholder="10-digit mobile number" />
                </div>
                <div style={{ display: 'flex', gap: 'var(--space-3)' }}>
                  <button type="submit" className="btn btn-primary btn-sm" disabled={savingProfile}>
                    <MdSave size={16} /> {savingProfile ? 'Saving...' : 'Save Changes'}
                  </button>
                  <button type="button" className="btn btn-ghost btn-sm" onClick={() => setEditingProfile(false)}>
                    <MdClose size={16} /> Cancel
                  </button>
                </div>
              </form>
            ) : (
              <div className="profile-info-grid">
                <div className="profile-info-item">
                  <MdPerson size={18} color="var(--accent-primary)" />
                  <div>
                    <span className="info-label">Full Name</span>
                    <span className="info-value">{profile?.name}</span>
                  </div>
                </div>
                <div className="profile-info-item">
                  <MdEmail size={18} color="var(--accent-primary)" />
                  <div>
                    <span className="info-label">Email Address</span>
                    <span className="info-value">{profile?.email}</span>
                  </div>
                </div>
                <div className="profile-info-item">
                  <MdPhone size={18} color="var(--accent-primary)" />
                  <div>
                    <span className="info-label">Phone Number</span>
                    <span className="info-value">{profile?.phoneNumber || <span className="text-muted">Not added</span>}</span>
                  </div>
                </div>
                <div className="profile-info-item">
                  <MdStar size={18} color="var(--accent-secondary)" />
                  <div>
                    <span className="info-label">Member Since</span>
                    <span className="info-value">{new Date(profile?.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' })}</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* ── Addresses Tab ── */}
        {activeTab === 'addresses' && (
          <div className="profile-section">
            <div className="profile-section-header">
              <h2>Saved Addresses</h2>
              <button className="btn btn-primary btn-sm" onClick={() => { setEditingAddress(null); setAddressForm({ name: '', phoneNumber: '', addressLine1: '', addressLine2: '', city: '', state: '', pincode: '', isDefault: false }); setShowAddressForm(true); }}>
                <MdAdd size={16} /> Add Address
              </button>
            </div>

            {showAddressForm && (
              <form onSubmit={handleSaveAddress} className="address-form-profile">
                <h3>{editingAddress ? 'Edit Address' : 'New Address'}</h3>
                <div className="address-form-grid-2">
                  <div className="form-group">
                    <label className="form-label">Full Name *</label>
                    <input className="form-input" value={addressForm.name} onChange={e => setAddressForm({ ...addressForm, name: e.target.value })} required />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Phone *</label>
                    <input className="form-input" value={addressForm.phoneNumber} onChange={e => setAddressForm({ ...addressForm, phoneNumber: e.target.value })} required />
                  </div>
                  <div className="form-group full-width">
                    <label className="form-label">Address Line 1 *</label>
                    <input className="form-input" value={addressForm.addressLine1} onChange={e => setAddressForm({ ...addressForm, addressLine1: e.target.value })} required />
                  </div>
                  <div className="form-group full-width">
                    <label className="form-label">Address Line 2</label>
                    <input className="form-input" value={addressForm.addressLine2} onChange={e => setAddressForm({ ...addressForm, addressLine2: e.target.value })} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">City *</label>
                    <input className="form-input" value={addressForm.city} onChange={e => setAddressForm({ ...addressForm, city: e.target.value })} required />
                  </div>
                  <div className="form-group">
                    <label className="form-label">State *</label>
                    <input className="form-input" value={addressForm.state} onChange={e => setAddressForm({ ...addressForm, state: e.target.value })} required />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Pincode *</label>
                    <input className="form-input" value={addressForm.pincode} onChange={e => setAddressForm({ ...addressForm, pincode: e.target.value })} required />
                  </div>
                  <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <input type="checkbox" id="isDefaultProfile" checked={addressForm.isDefault} onChange={e => setAddressForm({ ...addressForm, isDefault: e.target.checked })} />
                    <label htmlFor="isDefaultProfile" className="form-label" style={{ margin: 0, cursor: 'pointer' }}>Set as default</label>
                  </div>
                </div>
                <div style={{ display: 'flex', gap: 8, marginTop: 'var(--space-4)' }}>
                  <button type="submit" className="btn btn-primary btn-sm" disabled={savingAddress}>
                    <MdSave size={16} /> {savingAddress ? 'Saving...' : 'Save Address'}
                  </button>
                  <button type="button" className="btn btn-ghost btn-sm" onClick={() => { setShowAddressForm(false); setEditingAddress(null); }}>
                    <MdClose size={16} /> Cancel
                  </button>
                </div>
              </form>
            )}

            {addresses.length === 0 && !showAddressForm ? (
              <div className="empty-state" style={{ padding: 'var(--space-10)' }}>
                <MdLocationOn size={48} color="var(--text-muted)" />
                <p className="empty-state-title">No addresses saved</p>
                <p className="empty-state-desc">Add a delivery address to make checkout faster</p>
              </div>
            ) : (
              <div className="addresses-grid">
                {addresses.map(addr => (
                  <div key={addr.id} className={`address-card-profile ${addr.isDefault ? 'default' : ''}`}>
                    {addr.isDefault && <span className="badge badge-green address-default-badge">Default</span>}
                    <div className="address-card-name">{addr.name}</div>
                    <div className="address-card-detail">{addr.phoneNumber}</div>
                    <div className="address-card-detail">
                      {addr.addressLine1}{addr.addressLine2 && `, ${addr.addressLine2}`}
                    </div>
                    <div className="address-card-detail">{addr.city}, {addr.state} - {addr.pincode}</div>
                    <div className="address-card-actions">
                      <button className="btn btn-ghost btn-sm" onClick={() => openEditAddress(addr)}>
                        <MdEdit size={14} /> Edit
                      </button>
                      {!addr.isDefault && (
                        <button className="btn btn-ghost btn-sm" onClick={() => handleSetDefault(addr.id)}>
                          Set Default
                        </button>
                      )}
                      <button className="btn btn-sm" style={{ background: 'rgba(244,67,54,0.1)', color: 'var(--accent-red)', border: '1px solid rgba(244,67,54,0.2)' }}
                        onClick={() => handleDeleteAddress(addr.id)}>
                        <MdDelete size={14} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* ── Rewards Tab ── */}
        {activeTab === 'rewards' && (
          <div className="profile-section">
            <div className="profile-section-header">
              <h2>My Rewards</h2>
            </div>
            <RewardsWallet />
          </div>
        )}

        {/* ── Security Tab ── */}
        {activeTab === 'security' && (
          <div className="profile-section">
            <div className="profile-section-header">
              <h2>Change Password</h2>
            </div>
            {profile?.provider !== 'LOCAL' ? (
              <div className="empty-state" style={{ padding: 'var(--space-10)' }}>
                <SiGoogle size={48} color="var(--text-muted)" />
                <p className="empty-state-title">Google Account</p>
                <p className="empty-state-desc">You signed in with Google — password change is not available for Google accounts</p>
              </div>
            ) : (
              <form onSubmit={handleChangePassword} className="profile-form">
                <div className="form-group">
                  <label className="form-label">Current Password</label>
                  <input type="password" className="form-input" value={passwordForm.currentPassword}
                    onChange={e => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })} required />
                </div>
                <div className="form-group">
                  <label className="form-label">New Password</label>
                  <input type="password" className="form-input" value={passwordForm.newPassword}
                    onChange={e => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                    placeholder="Min 8 chars, 1 uppercase, 1 number, 1 special" required />
                </div>
                <div className="form-group">
                  <label className="form-label">Confirm New Password</label>
                  <input type="password" className="form-input" value={passwordForm.confirmPassword}
                    onChange={e => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })} required />
                </div>
                <button type="submit" className="btn btn-primary" disabled={savingPassword}>
                  <MdLock size={16} /> {savingPassword ? 'Changing...' : 'Change Password'}
                </button>
              </form>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default ProfilePage;