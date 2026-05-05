// src/components/common/Loader.jsx
import './Loader.css';

export const Loader = ({ fullPage = false }) => {
  if (fullPage) {
    return (
      <div className="loader-fullpage">
        <div className="spinner" />
        <p className="loader-text">Loading...</p>
      </div>
    );
  }
  return (
    <div className="loader-inline">
      <div className="spinner" />
    </div>
  );
};

export default Loader;
