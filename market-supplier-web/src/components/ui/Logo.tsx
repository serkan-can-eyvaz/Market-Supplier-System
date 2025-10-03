import React from 'react';
import { Box, useTheme, useMediaQuery } from '@mui/material';
import { BarChart3, Shield, Store, Building2, User } from 'lucide-react';

export const LogoIcon: React.FC<{ size?: number; color?: string; bg?: string; children?: React.ReactNode }> = ({ size = 28, color = 'white', bg, children }) => {
  const theme = useTheme();
  const isXs = useMediaQuery(theme.breakpoints.down('sm'));
  const isMdUp = useMediaQuery(theme.breakpoints.up('md'));

  const base = size;
  const currentSize = isMdUp ? base * 1.2 : isXs ? Math.max(22, base * 0.8) : base * 1.05;
  const iconSize = Math.floor(currentSize * 0.55);

  return (
    <Box
      sx={{
        width: currentSize,
        height: currentSize,
        borderRadius: '50%',
        background: bg || 'linear-gradient(135deg, #fdfdfd 0%, #f2f5ff 100%)',
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.08)',
        border: '1px solid rgba(0, 0, 0, 0.04)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
        flex: '0 0 auto',
      }}
      data-logo
    >
      {children ?? <BarChart3 size={iconSize} color="#667eea" />}
    </Box>
  );
};

export const UserAvatar: React.FC<{ size?: number; role?: string }> = ({ size = 44, role = 'supplier' }) => {
  const theme = useTheme();
  const isXs = useMediaQuery(theme.breakpoints.down('sm'));
  const isMdUp = useMediaQuery(theme.breakpoints.up('md'));

  const base = size;
  const currentSize = isMdUp ? base : isXs ? Math.max(34, base * 0.8) : base;
  const iconSize = Math.floor(currentSize * 0.42);

  const getIcon = () => {
    switch ((role || '').toUpperCase()) {
      case 'ADMIN':
        return <Shield size={iconSize} color="white" />;
      case 'MARKET':
        return <Store size={iconSize} color="white" />;
      case 'SUPPLIER':
        return <Building2 size={iconSize} color="white" />;
      default:
        return <User size={iconSize} color="white" />;
    }
  };

  return (
    <Box
      sx={{
        width: currentSize,
        height: currentSize,
        borderRadius: '50%',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        boxShadow: '0 6px 18px rgba(102, 126, 234, 0.35)',
        border: '1px solid rgba(255, 255, 255, 0.25)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
        flex: '0 0 auto',
        transition: 'transform 0.2s ease',
        '&:hover': { transform: { xs: 'none', sm: 'scale(1.03)' } },
      }}
      data-logo
    >
      {getIcon()}
    </Box>
  );
};

export default LogoIcon;
