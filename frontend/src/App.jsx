import { BrowserRouter, Outlet, Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import AdminDashboard from './pages/AdminDashboard';
import Checkout from './pages/Checkout';
import CustomerDashboard from './pages/CustomerDashboard';
import Home from './pages/Home';
import HostDashboard from './pages/HostDashboard';
import Login from './pages/Login';
import Notifications from './pages/Notifications';
import NotFound from './pages/NotFound';
import Profile from './pages/Profile';
import Properties from './pages/Properties';
import PropertyDetails from './pages/PropertyDetails';
import Register from './pages/Register';
import Unauthorized from './pages/Unauthorized';

function SiteLayout() {
  return <><Navbar /><Outlet /></>;
}

export default function App() {
  return <BrowserRouter><Routes>
    <Route element={<SiteLayout />}>
      <Route path="/" element={<Home />} />
      <Route path="/properties" element={<Properties />} />
      <Route path="/properties/:id" element={<PropertyDetails />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/unauthorized" element={<Unauthorized />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/profile" element={<Profile />} />
        <Route path="/notifications" element={<Notifications />} />
        <Route path="/checkout/:bookingId" element={<Checkout />} />
      </Route>
      <Route element={<ProtectedRoute roles={['CUSTOMER']} />}>
        <Route path="/dashboard" element={<CustomerDashboard />} />
        <Route path="/customer" element={<CustomerDashboard />} />
      </Route>
      <Route element={<ProtectedRoute roles={['HOST']} />}>
        <Route path="/host" element={<HostDashboard />} />
      </Route>
      <Route element={<ProtectedRoute roles={['ADMIN']} />}>
        <Route path="/admin" element={<AdminDashboard />} />
      </Route>
      <Route path="*" element={<NotFound />} />
    </Route>
  </Routes></BrowserRouter>;
}
