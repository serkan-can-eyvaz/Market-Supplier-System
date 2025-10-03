import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';

void main() {
  runApp(const MarketSupplierApp());
}

class MarketSupplierApp extends StatelessWidget {
  const MarketSupplierApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        // Auth Provider
        // ChangeNotifierProvider(create: (_) => AuthProvider()),
        // Order Provider
        // ChangeNotifierProvider(create: (_) => OrderProvider()),
      ],
      child: MaterialApp.router(
        title: 'Market Supplier System',
        theme: ThemeData(
          primarySwatch: Colors.blue,
          visualDensity: VisualDensity.adaptivePlatformDensity,
        ),
        routerConfig: _router,
      ),
    );
  }
}

// Router Configuration
final GoRouter _router = GoRouter(
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const WelcomeScreen(),
    ),
    GoRoute(
      path: '/login',
      builder: (context, state) => const LoginScreen(),
    ),
    GoRoute(
      path: '/market-dashboard',
      builder: (context, state) => const MarketDashboardScreen(),
    ),
    GoRoute(
      path: '/supplier-dashboard',
      builder: (context, state) => const SupplierDashboardScreen(),
    ),
  ],
);

// Placeholder Screens
class WelcomeScreen extends StatelessWidget {
  const WelcomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Market Supplier System'),
      ),
      body: const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              'Hoş Geldiniz!',
              style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
            ),
            SizedBox(height: 20),
            Text('Market Supplier System'),
            SizedBox(height: 40),
            // Login buttons will be added here
          ],
        ),
      ),
    );
  }
}

class LoginScreen extends StatelessWidget {
  const LoginScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Giriş Yap'),
      ),
      body: const Center(
        child: Text('Login Screen - Coming Soon'),
      ),
    );
  }
}

class MarketDashboardScreen extends StatelessWidget {
  const MarketDashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Market Paneli'),
      ),
      body: const Center(
        child: Text('Market Dashboard - Coming Soon'),
      ),
    );
  }
}

class SupplierDashboardScreen extends StatelessWidget {
  const SupplierDashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Tedarikçi Paneli'),
      ),
      body: const Center(
        child: Text('Supplier Dashboard - Coming Soon'),
      ),
    );
  }
}