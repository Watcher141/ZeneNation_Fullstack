// src/pages/auth/OAuth2CallbackPage.jsx
import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { userApi } from '../../api/apiCollections';
import Loader from '../../components/common/Loader';
import toast from 'react-hot-toast';

const OAuth2CallbackPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { setUser } = useAuth();

  useEffect(() => {
    const accessToken = searchParams.get('accessToken');
    const refreshToken = searchParams.get('refreshToken');

    if (accessToken && refreshToken) {
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);

      // Fetch user profile with the new token
      userApi.getProfile()
        .then(res => {
          const user = { ...res.data.data, accessToken, refreshToken };
          localStorage.setItem('user', JSON.stringify(user));
          setUser(user);
          toast.success(`Welcome, ${user.name}!`);
          {/**Changing navigate to '/home' to '/' after google reg   -- 6/3/2026 */}
          navigate(user.role === 'ROLE_ADMIN' ? '/admin' : '/');
        })
        .catch(() => {
          toast.error('Login failed. Please try again.');
          navigate('/login');
        });
    } else {
      toast.error('Google login failed');
      navigate('/login');
    }
  }, []);

  return <Loader fullPage />;
};

export default OAuth2CallbackPage;