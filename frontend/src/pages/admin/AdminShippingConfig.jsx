import { useState, useEffect } from 'react';
import AdminLayout from '../../components/admin/AdminLayout';
import Loader from '../../components/common/Loader';
import { shippingApi } from '../../api/apiCollections';
import toast from 'react-hot-toast';
import { MdLocalShipping, MdDelete, MdAdd, MdSave, MdRefresh } from 'react-icons/md';
import { BsCash } from 'react-icons/bs';

const AdminShippingConfig = () => {
  const [deliverySlabs, setDeliverySlabs] = useState([]);
  const [codSlabs, setCodSlabs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [savingDelivery, setSavingDelivery] = useState(false);
  const [savingCod, setSavingCod] = useState(false);

  const fetchConfig = async () => {
    setLoading(true);
    try {
      const res = await shippingApi.getConfig();
      const data = res.data.data;
      // Sort slabs to present them cleanly in order
      setDeliverySlabs(
        (data.deliverySlabs || []).sort((a, b) => a.minWeightGrams - b.minWeightGrams)
      );
      setCodSlabs(
        (data.codSlabs || []).sort((a, b) => Number(a.minOrderAmount) - Number(b.minOrderAmount))
      );
    } catch (err) {
      toast.error('Failed to load shipping configurations');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConfig();
  }, []);

  // Delivery Slab Handlers
  const handleAddDeliverySlab = () => {
    setDeliverySlabs(prev => [
      ...prev,
      { minWeightGrams: 0, maxWeightGrams: 0, charge: 0 }
    ]);
  };

  const handleDeliverySlabChange = (index, field, value) => {
    setDeliverySlabs(prev => {
      const next = [...prev];
      next[index] = { ...next[index], [field]: Number(value) };
      return next;
    });
  };

  const handleDeleteDeliverySlab = (index) => {
    setDeliverySlabs(prev => prev.filter((_, i) => i !== index));
  };

  const handleSaveDeliverySlabs = async () => {
    // Basic validation
    for (const slab of deliverySlabs) {
      if (slab.minWeightGrams < 0 || slab.maxWeightGrams < 0 || slab.charge < 0) {
        toast.error('Values cannot be negative');
        return;
      }
      if (slab.minWeightGrams > slab.maxWeightGrams) {
        toast.error('Min weight cannot be greater than Max weight');
        return;
      }
    }

    setSavingDelivery(true);
    try {
      const sorted = [...deliverySlabs].sort((a, b) => a.minWeightGrams - b.minWeightGrams);
      const res = await shippingApi.updateDeliverySlabs(sorted);
      setDeliverySlabs(
        (res.data.data?.deliverySlabs || []).sort((a, b) => a.minWeightGrams - b.minWeightGrams)
      );
      toast.success('Delivery charge slabs updated successfully!');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update delivery slabs');
    } finally {
      setSavingDelivery(false);
    }
  };

  // COD Slab Handlers
  const handleAddCodSlab = () => {
    setCodSlabs(prev => [
      ...prev,
      { minOrderAmount: 0, maxOrderAmount: 0, extraCharge: 0 }
    ]);
  };

  const handleCodSlabChange = (index, field, value) => {
    setCodSlabs(prev => {
      const next = [...prev];
      next[index] = { ...next[index], [field]: Number(value) };
      return next;
    });
  };

  const handleDeleteCodSlab = (index) => {
    setCodSlabs(prev => prev.filter((_, i) => i !== index));
  };

  const handleSaveCodSlabs = async () => {
    // Basic validation
    for (const slab of codSlabs) {
      if (slab.minOrderAmount < 0 || slab.maxOrderAmount < 0 || slab.extraCharge < 0) {
        toast.error('Values cannot be negative');
        return;
      }
      if (Number(slab.minOrderAmount) > Number(slab.maxOrderAmount)) {
        toast.error('Min order amount cannot be greater than Max order amount');
        return;
      }
    }

    setSavingCod(true);
    try {
      const sorted = [...codSlabs].sort((a, b) => Number(a.minOrderAmount) - Number(b.minOrderAmount));
      const res = await shippingApi.updateCodSlabs(sorted);
      setCodSlabs(
        (res.data.data?.codSlabs || []).sort((a, b) => Number(a.minOrderAmount) - Number(b.minOrderAmount))
      );
      toast.success('COD charge slabs updated successfully!');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update COD slabs');
    } finally {
      setSavingCod(false);
    }
  };

  if (loading) {
    return (
      <AdminLayout>
        <Loader fullPage />
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
      <div className="admin-page-header" style={{ marginBottom: 'var(--space-6)' }}>
        <div>
          <h1 className="admin-page-title">Shipping Configuration</h1>
          <p className="admin-page-subtitle">Configure delivery charges by weight slabs and COD extra charges by subtotal.</p>
        </div>
        <button className="btn btn-ghost" onClick={fetchConfig} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <MdRefresh size={18} /> Refresh
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-6)', alignItems: 'start' }}>
        
        {/* ── Delivery Charge Slabs ── */}
        <div className="admin-card" style={{ background: 'var(--bg-secondary)', padding: 'var(--space-5)', borderRadius: 'var(--border-radius-lg)', border: '1px solid var(--border-color)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-4)' }}>
            <h3 style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '1.1rem', fontWeight: 600 }}>
              <MdLocalShipping size={20} color="var(--accent-primary)" /> Delivery Charge Slabs
            </h3>
            <button className="btn btn-ghost btn-sm" onClick={handleAddDeliverySlab} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <MdAdd size={16} /> Add Slab
            </button>
          </div>
          
          <div style={{ overflowX: 'auto' }}>
            <table className="admin-table" style={{ width: '100%' }}>
              <thead>
                <tr>
                  <th>Min Weight (g)</th>
                  <th>Max Weight (g)</th>
                  <th>Charge (₹)</th>
                  <th style={{ width: 50 }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {deliverySlabs.length === 0 ? (
                  <tr>
                    <td colSpan={4} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>
                      No delivery slabs configured. (Default: Free delivery)
                    </td>
                  </tr>
                ) : (
                  deliverySlabs.map((slab, index) => (
                    <tr key={index}>
                      <td>
                        <input
                          type="number"
                          className="form-input"
                          style={{ padding: '4px 8px', fontSize: '14px' }}
                          value={slab.minWeightGrams}
                          onChange={(e) => handleDeliverySlabChange(index, 'minWeightGrams', e.target.value)}
                        />
                      </td>
                      <td>
                        <input
                          type="number"
                          className="form-input"
                          style={{ padding: '4px 8px', fontSize: '14px' }}
                          value={slab.maxWeightGrams}
                          onChange={(e) => handleDeliverySlabChange(index, 'maxWeightGrams', e.target.value)}
                        />
                      </td>
                      <td>
                        <input
                          type="number"
                          className="form-input"
                          style={{ padding: '4px 8px', fontSize: '14px' }}
                          value={slab.charge}
                          onChange={(e) => handleDeliverySlabChange(index, 'charge', e.target.value)}
                        />
                      </td>
                      <td>
                        <button
                          className="btn btn-ghost btn-sm"
                          style={{ color: 'var(--accent-red)' }}
                          onClick={() => handleDeleteDeliverySlab(index)}
                        >
                          <MdDelete size={18} />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <div style={{ marginTop: 'var(--space-4)', display: 'flex', justifyContent: 'flex-end' }}>
            <button
              className="btn btn-primary"
              onClick={handleSaveDeliverySlabs}
              disabled={savingDelivery}
              style={{ display: 'flex', alignItems: 'center', gap: 6 }}
            >
              <MdSave size={18} /> {savingDelivery ? 'Saving...' : 'Save Slabs'}
            </button>
          </div>
        </div>

        {/* ── COD Extra Charge Slabs ── */}
        <div className="admin-card" style={{ background: 'var(--bg-secondary)', padding: 'var(--space-5)', borderRadius: 'var(--border-radius-lg)', border: '1px solid var(--border-color)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-4)' }}>
            <h3 style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '1.1rem', fontWeight: 600 }}>
              <BsCash size={20} color="var(--accent-secondary)" /> COD Extra Charge Slabs
            </h3>
            <button className="btn btn-ghost btn-sm" onClick={handleAddCodSlab} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <MdAdd size={16} /> Add Slab
            </button>
          </div>

          <div style={{ overflowX: 'auto' }}>
            <table className="admin-table" style={{ width: '100%' }}>
              <thead>
                <tr>
                  <th>Min Order (₹)</th>
                  <th>Max Order (₹)</th>
                  <th>Extra Charge (₹)</th>
                  <th style={{ width: 50 }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {codSlabs.length === 0 ? (
                  <tr>
                    <td colSpan={4} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>
                      No COD slabs configured. (Default: No COD surcharge)
                    </td>
                  </tr>
                ) : (
                  codSlabs.map((slab, index) => (
                    <tr key={index}>
                      <td>
                        <input
                          type="number"
                          className="form-input"
                          style={{ padding: '4px 8px', fontSize: '14px' }}
                          value={slab.minOrderAmount}
                          onChange={(e) => handleCodSlabChange(index, 'minOrderAmount', e.target.value)}
                        />
                      </td>
                      <td>
                        <input
                          type="number"
                          className="form-input"
                          style={{ padding: '4px 8px', fontSize: '14px' }}
                          value={slab.maxOrderAmount}
                          onChange={(e) => handleCodSlabChange(index, 'maxOrderAmount', e.target.value)}
                        />
                      </td>
                      <td>
                        <input
                          type="number"
                          className="form-input"
                          style={{ padding: '4px 8px', fontSize: '14px' }}
                          value={slab.extraCharge}
                          onChange={(e) => handleCodSlabChange(index, 'extraCharge', e.target.value)}
                        />
                      </td>
                      <td>
                        <button
                          className="btn btn-ghost btn-sm"
                          style={{ color: 'var(--accent-red)' }}
                          onClick={() => handleDeleteCodSlab(index)}
                        >
                          <MdDelete size={18} />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <div style={{ marginTop: 'var(--space-4)', display: 'flex', justifyContent: 'flex-end' }}>
            <button
              className="btn btn-secondary"
              onClick={handleSaveCodSlabs}
              disabled={savingCod}
              style={{ display: 'flex', alignItems: 'center', gap: 6 }}
            >
              <MdSave size={18} /> {savingCod ? 'Saving...' : 'Save Slabs'}
            </button>
          </div>
        </div>

      </div>
    </AdminLayout>
  );
};

export default AdminShippingConfig;
