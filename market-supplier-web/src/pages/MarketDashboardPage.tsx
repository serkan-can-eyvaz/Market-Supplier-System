import React from 'react';
import { Box, Typography, Card, CardContent } from '@mui/material';

const MarketDashboardPage: React.FC = () => {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Market Dashboard
      </Typography>
      <Card>
        <CardContent>
          <Typography variant="h6">
            Market sahibi için özel dashboard sayfası
          </Typography>
          <Typography variant="body2" color="textSecondary">
            Bu sayfa market sahipleri için özel olarak tasarlanacak.
          </Typography>
        </CardContent>
      </Card>
    </Box>
  );
};

export default MarketDashboardPage;
