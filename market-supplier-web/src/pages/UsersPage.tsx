import React, { useEffect, useMemo, useState } from 'react';
import {
  Box,
  Typography,
  TextField,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Alert,
  Avatar,
  Fade,
  Slide,
  IconButton,
  Tooltip,
  InputAdornment,
  Stack,
  Button,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Divider,
} from '@mui/material';
import {
  Search as SearchIcon,
  FilterList as FilterIcon,
  MoreVert as MoreVertIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  PersonAdd as PersonAddIcon,
  Refresh as RefreshIcon,
  AdminPanelSettings as AdminIcon,
  Storefront as StorefrontIcon,
  BusinessCenter as BusinessCenterIcon,
  People as PeopleIcon,
  Email as EmailIcon,
  CalendarToday as CalendarIcon,
  Visibility as VisibilityIcon,
} from '@mui/icons-material';
import apiService from '../services/api';
import { User } from '../types';

// Professional Logo Component
const LogoIcon = ({ size = 48, children, bg }: { size?: number; children: React.ReactNode; bg: string }) => (
  <Box
    sx={{
      width: size,
      height: size,
      borderRadius: '50%',
      background: bg,
      boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
      border: '2px solid rgba(255,255,255,0.25)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }}
  >
    {children}
  </Box>
);

const UsersPage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [query, setQuery] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [roleFilter, setRoleFilter] = useState<string>('ALL');
  const [showUserDialog, setShowUserDialog] = useState<boolean>(false);
  const [showEditDialog, setShowEditDialog] = useState<boolean>(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState<boolean>(false);
  const [editForm, setEditForm] = useState<{ name: string; email: string }>({ name: '', email: '' });

  const getRoleIcon = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return <AdminIcon sx={{ fontSize: 20 }} />;
      case 'MARKET':
        return <StorefrontIcon sx={{ fontSize: 20 }} />;
      case 'SUPPLIER':
        return <BusinessCenterIcon sx={{ fontSize: 20 }} />;
      default:
        return <PeopleIcon sx={{ fontSize: 20 }} />;
    }
  };

  const getRoleColor = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return '#f44336';
      case 'MARKET':
        return '#f093fb';
      case 'SUPPLIER':
        return '#4facfe';
      default:
        return '#43e97b';
    }
  };

  const getRoleText = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'Yönetici';
      case 'MARKET':
        return 'Market';
      case 'SUPPLIER':
        return 'Tedarikçi';
      default:
        return 'Kullanıcı';
    }
  };

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, user: User) => {
    setAnchorEl(event.currentTarget);
    setSelectedUser(user);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedUser(null);
  };

  const handleViewUser = async (user: User) => {
    console.log('handleViewUser called with user:', user);
    try {
      const userDetails = await apiService.getUserById(user.id);
      console.log('User details fetched:', userDetails);
      setSelectedUser(userDetails);
      setShowUserDialog(true);
      console.log('showUserDialog set to true');
    } catch (e: any) {
      console.error('Error fetching user details:', e);
      setError(e.response?.data?.message || 'Kullanıcı detayları yüklenemedi');
    }
    handleMenuClose();
  };

  const handleEditUser = (user: User) => {
    console.log('handleEditUser called with user:', user);
    setSelectedUser(user);
    setEditForm({ name: user.name, email: user.email });
    setShowEditDialog(true);
    console.log('showEditDialog set to true');
    handleMenuClose();
  };

  const handleDeleteUser = (user: User) => {
    setSelectedUser(user);
    setShowDeleteDialog(true);
    handleMenuClose();
  };

  const handleUpdateUser = async () => {
    if (!selectedUser) return;
    
    try {
      setLoading(true);
      const updatedUser = await apiService.updateUser(selectedUser.id, editForm);
      setUsers(users.map(u => u.id === selectedUser.id ? updatedUser : u));
      setShowEditDialog(false);
      setSelectedUser(null);
      setEditForm({ name: '', email: '' });
    } catch (e: any) {
      setError(e.response?.data?.message || 'Kullanıcı güncellenemedi');
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!selectedUser) return;
    
    try {
      setLoading(true);
      await apiService.deleteUser(selectedUser.id);
      setUsers(users.filter(u => u.id !== selectedUser.id));
      setShowDeleteDialog(false);
      setSelectedUser(null);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Kullanıcı silinemedi');
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    setLoading(true);
    try {
      setError('');
      const data = await apiService.getUsers();
      setUsers(data);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Kullanıcılar yüklenemedi');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    handleRefresh();
  }, []);

  const filtered = useMemo(() => {
    let filteredUsers = users;
    
    // Role filter
    if (roleFilter !== 'ALL') {
      filteredUsers = filteredUsers.filter(u => u.role === roleFilter);
    }
    
    // Search filter
    const q = query.trim().toLowerCase();
    if (q) {
      filteredUsers = filteredUsers.filter(u =>
        u.name.toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q) ||
        u.role.toLowerCase().includes(q)
      );
    }
    
    return filteredUsers;
  }, [users, query, roleFilter]);

  console.log('Current state:', { showUserDialog, showEditDialog, showDeleteDialog, selectedUser });
  
  return (
    <Box sx={{ minHeight: '100vh', background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)' }}>
      {/* Header Section */}
      <Slide direction="down" in timeout={600}>
        <Paper
          elevation={0}
          sx={{
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            borderRadius: 3,
            p: 4,
            mb: 4,
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 3 }}>
              <LogoIcon size={64} bg={'linear-gradient(135deg, #667eea, #764ba2)'}>
                <PeopleIcon sx={{ fontSize: 32, color: 'white' }} />
              </LogoIcon>
              <Box>
                <Typography variant="h3" fontWeight="800" sx={{ mb: 1 }}>
                  Kullanıcılar
                </Typography>
                <Typography variant="h6" color="text.secondary" sx={{ fontWeight: 500 }}>
                  Sistem kullanıcılarını yönetin
                </Typography>
              </Box>
            </Box>
            <Stack direction="row" spacing={2}>
              <Tooltip title="Verileri Yenile">
                <IconButton
                  onClick={handleRefresh}
                  disabled={loading}
                  sx={{
                    background: 'rgba(102, 126, 234, 0.1)',
                    color: 'primary.main',
                    '&:hover': {
                      background: 'rgba(102, 126, 234, 0.2)',
                      transform: 'scale(1.05)',
                    },
                  }}
                >
                  <RefreshIcon />
                </IconButton>
              </Tooltip>
              <Button
                variant="contained"
                startIcon={<PersonAddIcon />}
                sx={{
                  background: 'linear-gradient(135deg, #667eea, #764ba2)',
                  borderRadius: 2,
                  px: 3,
                  py: 1.5,
                  fontWeight: 600,
                  textTransform: 'none',
                  boxShadow: '0 8px 25px rgba(102, 126, 234, 0.3)',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #5a6fd8, #6a4190)',
                    transform: 'translateY(-2px)',
                    boxShadow: '0 12px 35px rgba(102, 126, 234, 0.4)',
                  },
                }}
              >
                Yeni Kullanıcı
              </Button>
            </Stack>
          </Box>

          {/* Search and Filter Section */}
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
            <TextField
              fullWidth
              placeholder="Ara (isim, e-posta, rol)"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon sx={{ color: 'text.secondary' }} />
                  </InputAdornment>
                ),
              }}
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: 2,
                  background: 'rgba(255, 255, 255, 0.8)',
                  backdropFilter: 'blur(10px)',
                  '&:hover': {
                    background: 'rgba(255, 255, 255, 0.9)',
                  },
                  '&.Mui-focused': {
                    background: 'rgba(255, 255, 255, 1)',
                  },
                },
              }}
            />
            <Button
              variant="outlined"
              startIcon={<FilterIcon />}
              onClick={() => setRoleFilter(roleFilter === 'ALL' ? 'ADMIN' : roleFilter === 'ADMIN' ? 'MARKET' : roleFilter === 'MARKET' ? 'SUPPLIER' : 'ALL')}
              sx={{
                borderRadius: 2,
                px: 3,
                py: 1.5,
                fontWeight: 600,
                textTransform: 'none',
                borderColor: 'rgba(102, 126, 234, 0.3)',
                color: 'primary.main',
                '&:hover': {
                  borderColor: 'primary.main',
                  background: 'rgba(102, 126, 234, 0.05)',
                },
              }}
            >
              {roleFilter === 'ALL' ? 'Tüm Roller' : getRoleText(roleFilter)}
            </Button>
          </Box>
        </Paper>
      </Slide>

      {/* Error Alert */}
      {error && (
        <Slide direction="down" in timeout={800}>
          <Alert 
            severity="error" 
            sx={{ 
              mb: 3, 
              borderRadius: 2,
              background: 'rgba(244, 67, 54, 0.1)',
              backdropFilter: 'blur(10px)',
              border: '1px solid rgba(244, 67, 54, 0.2)',
            }}
          >
            {error}
          </Alert>
        </Slide>
      )}

      {/* Users Table */}
      <Slide direction="up" in timeout={1000}>
        <Paper
          elevation={0}
          sx={{
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            borderRadius: 3,
            overflow: 'hidden',
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
          }}
        >
          <TableContainer
            sx={{
              '&::-webkit-scrollbar': {
                width: '12px',
                height: '12px',
              },
              '&::-webkit-scrollbar-track': {
                background: 'rgba(0, 0, 0, 0.1)',
                borderRadius: '6px',
              },
              '&::-webkit-scrollbar-thumb': {
                background: 'linear-gradient(135deg, #667eea, #764ba2)',
                borderRadius: '6px',
                border: '2px solid rgba(255, 255, 255, 0.2)',
                '&:hover': {
                  background: 'linear-gradient(135deg, #5a6fd8, #6a4190)',
                },
              },
              '&::-webkit-scrollbar-thumb:active': {
                background: 'linear-gradient(135deg, #4e5fc6, #5e377e)',
              },
              '&::-webkit-scrollbar-corner': {
                background: 'rgba(0, 0, 0, 0.1)',
              },
              // Firefox için
              scrollbarWidth: 'thin',
              scrollbarColor: '#667eea rgba(0, 0, 0, 0.1)',
            }}
          >
            <Table>
              <TableHead>
                <TableRow sx={{ background: 'linear-gradient(135deg, #667eea, #764ba2)' }}>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Kullanıcı
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Email
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Rol
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem' }}>
                    Oluşturulma
                  </TableCell>
                  <TableCell sx={{ color: 'white', fontWeight: 700, fontSize: '0.9rem', textAlign: 'center' }}>
                    İşlemler
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filtered.map((user, index) => (
                  <Fade in timeout={1200 + index * 100} key={user.id}>
                    <TableRow
                      sx={{
                        '&:hover': {
                          background: 'rgba(102, 126, 234, 0.05)',
                          transform: 'scale(1.01)',
                          boxShadow: '0 4px 15px rgba(102, 126, 234, 0.1)',
                        },
                        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                        borderBottom: '1px solid rgba(0, 0, 0, 0.05)',
                      }}
                    >
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                          <LogoIcon size={40} bg={`linear-gradient(135deg, ${getRoleColor(user.role)}, ${getRoleColor(user.role)}80)`}>
                            {getRoleIcon(user.role)}
                          </LogoIcon>
                          <Box>
                            <Typography variant="subtitle1" fontWeight="600">
                              {user.name}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              ID: {user.id}
                            </Typography>
                          </Box>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <EmailIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                          <Typography variant="body2">
                            {user.email}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          icon={getRoleIcon(user.role)}
                          label={getRoleText(user.role)}
                          size="small"
                          sx={{
                            background: getRoleColor(user.role),
                            color: 'white',
                            fontWeight: 600,
                            '& .MuiChip-icon': {
                              color: 'white',
                            },
                          }}
                        />
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <CalendarIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                          <Typography variant="body2">
                            {new Date(user.createdAt).toLocaleDateString('tr-TR', {
                              day: '2-digit',
                              month: '2-digit',
                              year: 'numeric',
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell sx={{ textAlign: 'center' }}>
                        <IconButton
                          onClick={(e) => handleMenuOpen(e, user)}
                          sx={{
                            color: 'text.secondary',
                            '&:hover': {
                              background: 'rgba(102, 126, 234, 0.1)',
                              color: 'primary.main',
                            },
                          }}
                        >
                          <MoreVertIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  </Fade>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      </Slide>

      {/* Action Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
        PaperProps={{
          sx: {
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)',
            borderRadius: 2,
            minWidth: 200,
          },
        }}
      >
        <MenuItem onClick={() => selectedUser && handleViewUser(selectedUser)}>
          <ListItemIcon>
            <VisibilityIcon fontSize="small" sx={{ color: 'primary.main' }} />
          </ListItemIcon>
          <ListItemText primary="Görüntüle" />
        </MenuItem>
        <MenuItem onClick={() => selectedUser && handleEditUser(selectedUser)}>
          <ListItemIcon>
            <EditIcon fontSize="small" sx={{ color: 'warning.main' }} />
          </ListItemIcon>
          <ListItemText primary="Düzenle" />
        </MenuItem>
        <Divider />
        <MenuItem onClick={() => selectedUser && handleDeleteUser(selectedUser)}>
          <ListItemIcon>
            <DeleteIcon fontSize="small" sx={{ color: 'error.main' }} />
          </ListItemIcon>
          <ListItemText primary="Sil" />
        </MenuItem>
      </Menu>

      {/* User Details Dialog */}
      {showUserDialog && selectedUser && (
        <Box
          sx={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
            p: 2,
          }}
        >
          <Paper
            elevation={24}
            sx={{
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              borderRadius: 3,
              p: 4,
              minWidth: 400,
              maxWidth: 500,
              width: '100%',
              boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)',
            }}
          >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
            <LogoIcon size={48} bg={`linear-gradient(135deg, ${getRoleColor(selectedUser.role)}, ${getRoleColor(selectedUser.role)}80)`}>
              {getRoleIcon(selectedUser.role)}
            </LogoIcon>
            <Box>
              <Typography variant="h5" fontWeight="700">
                {selectedUser.name}
              </Typography>
              <Chip
                icon={getRoleIcon(selectedUser.role)}
                label={getRoleText(selectedUser.role)}
                size="small"
                sx={{
                  background: getRoleColor(selectedUser.role),
                  color: 'white',
                  fontWeight: 600,
                  '& .MuiChip-icon': { color: 'white' },
                }}
              />
            </Box>
          </Box>
          
          <Box sx={{ mb: 3 }}>
            <Typography variant="body1" sx={{ mb: 1 }}>
              <strong>Email:</strong> {selectedUser.email}
            </Typography>
            <Typography variant="body1" sx={{ mb: 1 }}>
              <strong>ID:</strong> {selectedUser.id}
            </Typography>
            <Typography variant="body1" sx={{ mb: 1 }}>
              <strong>Oluşturulma:</strong> {new Date(selectedUser.createdAt).toLocaleDateString('tr-TR')}
            </Typography>
          </Box>
          
          <Button
            variant="contained"
            fullWidth
            onClick={() => setShowUserDialog(false)}
            sx={{
              background: 'linear-gradient(135deg, #667eea, #764ba2)',
              borderRadius: 2,
              py: 1.5,
              fontWeight: 600,
              textTransform: 'none',
            }}
          >
            Kapat
          </Button>
          </Paper>
        </Box>
      )}

      {/* Edit User Dialog */}
      {showEditDialog && selectedUser && (
        <Box
          sx={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
            p: 2,
          }}
        >
          <Paper
            elevation={24}
            sx={{
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              borderRadius: 3,
              p: 4,
              minWidth: 400,
              maxWidth: 500,
              width: '100%',
              boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)',
            }}
          >
          <Typography variant="h5" fontWeight="700" sx={{ mb: 3 }}>
            Kullanıcı Düzenle
          </Typography>
          
          <TextField
            fullWidth
            label="İsim"
            value={editForm.name}
            onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
            sx={{ mb: 2 }}
          />
          
          <TextField
            fullWidth
            label="Email"
            type="email"
            value={editForm.email}
            onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
            sx={{ mb: 3 }}
          />
          
          <Box sx={{ display: 'flex', gap: 2 }}>
            <Button
              variant="outlined"
              fullWidth
              onClick={() => setShowEditDialog(false)}
              sx={{ borderRadius: 2, py: 1.5 }}
            >
              İptal
            </Button>
            <Button
              variant="contained"
              fullWidth
              onClick={handleUpdateUser}
              disabled={loading}
              sx={{
                background: 'linear-gradient(135deg, #667eea, #764ba2)',
                borderRadius: 2,
                py: 1.5,
                fontWeight: 600,
                textTransform: 'none',
              }}
            >
              {loading ? 'Güncelleniyor...' : 'Güncelle'}
            </Button>
          </Box>
          </Paper>
        </Box>
      )}

      {/* Delete Confirmation Dialog */}
      {showDeleteDialog && selectedUser && (
        <Box
          sx={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
            p: 2,
          }}
        >
          <Paper
            elevation={24}
            sx={{
              background: 'rgba(255, 255, 255, 0.95)',
              backdropFilter: 'blur(20px)',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              borderRadius: 3,
              p: 4,
              minWidth: 400,
              maxWidth: 500,
              width: '100%',
              boxShadow: '0 20px 40px rgba(0, 0, 0, 0.15)',
            }}
          >
          <Typography variant="h5" fontWeight="700" sx={{ mb: 2, color: 'error.main' }}>
            Kullanıcıyı Sil
          </Typography>
          
          <Typography variant="body1" sx={{ mb: 3 }}>
            <strong>{selectedUser.name}</strong> kullanıcısını silmek istediğinizden emin misiniz? 
            Bu işlem geri alınamaz.
          </Typography>
          
          <Box sx={{ display: 'flex', gap: 2 }}>
            <Button
              variant="outlined"
              fullWidth
              onClick={() => setShowDeleteDialog(false)}
              sx={{ borderRadius: 2, py: 1.5 }}
            >
              İptal
            </Button>
            <Button
              variant="contained"
              fullWidth
              onClick={handleConfirmDelete}
              disabled={loading}
              sx={{
                background: 'linear-gradient(135deg, #f44336, #d32f2f)',
                borderRadius: 2,
                py: 1.5,
                fontWeight: 600,
                textTransform: 'none',
                '&:hover': {
                  background: 'linear-gradient(135deg, #d32f2f, #b71c1c)',
                },
              }}
            >
              {loading ? 'Siliniyor...' : 'Sil'}
            </Button>
          </Box>
          </Paper>
        </Box>
      )}

    </Box>
  );
};

export default UsersPage;
