// src/components/user/RewardsWallet.jsx
import { useState, useEffect, useCallback } from 'react';
import { rewardApi } from '../../api/apiCollections';
import { MdStar, MdTrendingUp, MdHistory, MdCardGiftcard } from 'react-icons/md';
import './RewardsWallet.css';

const RewardsWallet = () => {
  const [wallet, setWallet] = useState(null);
  const [history, setHistory] = useState([]);
  const [pagination, setPagination] = useState({});
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [showHistory, setShowHistory] = useState(false);

  const fetchWallet = useCallback(async () => {
    try {
      const res = await rewardApi.getWallet();
      setWallet(res.data.data);
    } catch { /* no wallet yet */ }
  }, []);

  const fetchHistory = useCallback(async () => {
    try {
      const res = await rewardApi.getHistory({ page, size: 8 });
      setHistory(res.data.data?.content || []);
      setPagination(res.data.data || {});
    } catch { /* silent */ }
    finally { setLoading(false); }
  }, [page]);

  useEffect(() => { fetchWallet(); }, [fetchWallet]);
  useEffect(() => { if (showHistory) fetchHistory(); }, [fetchHistory, showHistory]);

  return (
    <div className="rewards-wallet">
      {/* Balance Cards */}
      <div className="rewards-cards">
        <div className="rewards-card rewards-card-main">
          <div className="rewards-card-icon"><MdCardGiftcard size={32} /></div>
          <div className="rewards-balance">{wallet?.balance || 0}</div>
          <div className="rewards-label">Available Points</div>
          <div className="rewards-sublabel">= ₹{Math.floor((wallet?.balance || 0) / 2)} value  <span style={{ fontSize: 11, opacity: 0.7 }}>(2 pts = ₹1)</span></div>
        </div>
        <div className="rewards-card">
          <div className="rewards-card-icon"><MdTrendingUp size={24} /></div>
          <div className="rewards-balance rewards-balance-sm">{wallet?.lifetimeEarned || 0}</div>
          <div className="rewards-label">Total Earned</div>
        </div>
        <div className="rewards-card">
          <div className="rewards-card-icon"><MdStar size={24} /></div>
          <div className="rewards-balance rewards-balance-sm">{wallet?.lifetimeUsed || 0}</div>
          <div className="rewards-label">Total Redeemed</div>
        </div>
      </div>

      {/* How it works */}
      <div className="rewards-info">
        <h4><MdCardGiftcard size={16} /> How Rewards Work</h4>
        <ul>
          <li>Earn <strong>20% of purchase value as points</strong> after delivery<br />
            <span style={{ fontSize: 12, opacity: 0.8 }}>e.g. ₹500 order → 100 pts earned</span>
          </li>
          <li><strong>2 points = ₹1</strong> discount &nbsp;(100 pts = ₹50 value)</li>
          <li>Redeem up to <strong>60% of your balance</strong> per order<br />
            <span style={{ fontSize: 12, opacity: 0.8 }}>e.g. 200 pts balance → max 120 pts = ₹60 off</span>
          </li>
          <li>Minimum <strong>₹399 order</strong> required to redeem points</li>
          <li>Points expire after <strong>12 months</strong></li>
        </ul>
      </div>

      {/* History toggle */}
      <button className="btn btn-ghost btn-sm" style={{ display: 'flex', alignItems: 'center', gap: 6 }}
        onClick={() => setShowHistory(!showHistory)}>
        <MdHistory size={16} />
        {showHistory ? 'Hide History' : 'View Transaction History'}
      </button>

      {showHistory && (
        <div className="rewards-history">
          {loading ? (
            <p className="text-muted text-sm">Loading...</p>
          ) : history.length === 0 ? (
            <p className="text-muted text-sm">No transactions yet. Place an order to start earning!</p>
          ) : (
            <>
              {history.map(entry => (
                <div key={entry.id} className={`rewards-entry ${entry.transactionType === 'CREDIT' ? 'credit' : 'debit'}`}>
                  <div className="rewards-entry-info">
                    <span className="rewards-entry-reason">{entry.reason}</span>
                    {entry.orderNumber && (
                      <span className="text-xs text-muted">Order: {entry.orderNumber}</span>
                    )}
                    <span className="text-xs text-muted">
                      {new Date(entry.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
                      {entry.expiresAt && ` · Expires ${new Date(entry.expiresAt).toLocaleDateString('en-IN', { month: 'short', year: 'numeric' })}`}
                    </span>
                  </div>
                  <div className="rewards-entry-points">
                    <span className={entry.transactionType === 'CREDIT' ? 'text-success' : 'text-accent'}>
                      {entry.transactionType === 'CREDIT' ? '+' : '-'}{entry.points} pts
                    </span>
                    <span className="text-xs text-muted">Balance: {entry.balanceAfter}</span>
                  </div>
                </div>
              ))}

              {pagination.totalPages > 1 && (
                <div className="pagination" style={{ marginTop: 'var(--space-4)' }}>
                  <button className="btn btn-ghost btn-sm" disabled={pagination.isFirst} onClick={() => setPage(p => p - 1)}>← Prev</button>
                  <span className="text-muted text-xs">Page {page + 1} of {pagination.totalPages}</span>
                  <button className="btn btn-ghost btn-sm" disabled={pagination.isLast} onClick={() => setPage(p => p + 1)}>Next →</button>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
};

export default RewardsWallet;