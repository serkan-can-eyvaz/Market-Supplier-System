import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Box, Typography, Alert, Stack, Button, Paper, TextField } from '@mui/material';
import { useAuth } from '../contexts/AuthContext';
import apiService from '../services/api';
import { Delivery, RouteMetricsRequest } from '../types';
import { MAPBOX_CONFIG } from '../config';

type GeoPt = { lat: number; lon: number };

// Backend base URL (CRA dev server'da 3000 yerine 8080'e çağırmak için)
const API_BASE = (process.env.REACT_APP_API_URL as string) || 'http://localhost:8080/api';

const loadLeaflet = () =>
  new Promise<void>((resolve) => {
    if ((window as any).L) return resolve();
    const css = document.createElement('link');
    css.rel = 'stylesheet';
    css.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
    document.head.appendChild(css);
    const s = document.createElement('script');
    s.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
    s.onload = () => resolve();
    document.body.appendChild(s);
  });

async function geocodeAddress(address: string): Promise<GeoPt | null> {
  try {
    const url = `${API_BASE}/utils/geocode?q=${encodeURIComponent(address)}`; // backend proxy
    const res = await fetch(url);
    const data = await res.json();
    if (data && data.lat) return { lat: data.lat, lon: data.lon };
  } catch {}
  return null;
}

function haversine(a: GeoPt, b: GeoPt) {
  const toRad = (x: number) => (x * Math.PI) / 180;
  const R = 6371;
  const dLat = toRad(b.lat - a.lat);
  const dLon = toRad(b.lon - a.lon);
  const lat1 = toRad(a.lat);
  const lat2 = toRad(b.lat);
  const c =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
  const d = 2 * Math.atan2(Math.sqrt(c), Math.sqrt(1 - c));
  return R * d;
}

const DeliveriesMapPage: React.FC = () => {
  const { isAdmin, isSupplier } = useAuth();
  const [deliveries, setDeliveries] = useState<Delivery[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const mapRef = useRef<HTMLDivElement>(null);
  const map = useRef<any>(null);
  const markersRef = useRef<any[]>([]);
  const polylineRef = useRef<any>(null);
  const [sequence, setSequence] = useState<{ d: Delivery; pt: GeoPt }[]>([]);
  const [userLocation, setUserLocation] = useState<GeoPt | null>(null);
  const watchId = useRef<number | null>(null);
  const [totalDistanceKm, setTotalDistanceKm] = useState<number | null>(null);
  const [totalDurationMin, setTotalDurationMin] = useState<number | null>(null);
  const [nextDriveKm, setNextDriveKm] = useState<number | null>(null);
  const [nextDriveMin, setNextDriveMin] = useState<number | null>(null);
  const lastLegCalcRef = useRef<number>(0);
  const [fuelLPer100, setFuelLPer100] = useState<number>(8.5); // basit varsayım
  const [fuelPrice, setFuelPrice] = useState<number>(45); // TL/L
  const [routeStart, setRouteStart] = useState<'address' | 'mylocation'>('address');
  const [supplierAddress, setSupplierAddress] = useState<string>('');
  const [useAirline, setUseAirline] = useState<boolean>(false); // Yol rotası varsayılan
  const [isLoadingLocation, setIsLoadingLocation] = useState(false);
  const [isTrackingLocation, setIsTrackingLocation] = useState(false);
  const [currentDistanceToNext, setCurrentDistanceToNext] = useState<number | null>(null);
  const [currentDurationToNext, setCurrentDurationToNext] = useState<number | null>(null);
  const [isDrivingMode, setIsDrivingMode] = useState(false);
  const [drivingRoute, setDrivingRoute] = useState<any>(null);
  const [currentSpeed, setCurrentSpeed] = useState<number | null>(null);
  const [nextInstruction, setNextInstruction] = useState<string>('');
  const [distanceToNextTurn, setDistanceToNextTurn] = useState<number | null>(null);
  const [isNavigating, setIsNavigating] = useState(false);
  const [navigationStep, setNavigationStep] = useState<number>(0);
  const userLocationMarkerRef = useRef<any>(null);
  const navigationIntervalRef = useRef<NodeJS.Timeout | null>(null);

  // Konum alma fonksiyonu
  const getCurrentLocation = () => {
    if (!navigator.geolocation) {
      alert('Tarayıcınız konum erişimini desteklemiyor.');
      return;
    }

    console.log('Konum alınıyor...');
    setIsLoadingLocation(true);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude } = position.coords;
        console.log('Konum alındı:', latitude, longitude);
        setUserLocation({ lat: latitude, lon: longitude });
        setRouteStart('mylocation');
        setIsLoadingLocation(false);
        
        // Kullanıcı konumunu haritada göster
        showUserLocationOnMap({ lat: latitude, lon: longitude });
        
        // Konum alındıktan sonra rotayı çiz
        if (sequence.length > 0) {
          console.log('Konum alındı, rota çiziliyor...');
          drawRoute(sequence);
          // Real-time tracking başlat
          startRealTimeTracking(sequence);
          // Sürüş modunu başlat (konumdan ilk durağa)
          startDrivingMode({ lat: latitude, lon: longitude }, sequence[0].pt);
          // Google Maps benzeri navigasyon başlat
          startNavigation({ lat: latitude, lon: longitude }, sequence[0].pt);
        }
      },
      (error) => {
        console.error('Konum alınamadı:', error);
        setIsLoadingLocation(false);
        alert('Konum alınamadı. Lütfen tarayıcı ayarlarından konum erişimine izin verin.');
      },
      {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 300000 // 5 dakika
      }
    );
  };

  // Kullanıcı konumunu haritada göster
  const showUserLocationOnMap = (location: GeoPt) => {
    if (!(window as any).L || !map.current) return;
    const L = (window as any).L;

    // Eski marker'ı kaldır
    if (userLocationMarkerRef.current) {
      map.current.removeLayer(userLocationMarkerRef.current);
    }

    // Yeni marker ekle
    userLocationMarkerRef.current = L.marker([location.lat, location.lon], {
      icon: new L.DivIcon({
        html: '<div style="background:#4caf50;color:#fff;border-radius:50%;width:32px;height:32px;display:flex;align-items:center;justify-content:center;font-weight:bold;border:3px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,0.3);">📍</div>',
        className: 'user-location-marker'
      })
    }).addTo(map.current);

    userLocationMarkerRef.current.bindPopup(`
      <div style="min-width:200px;text-align:center;">
        <div style="background:#4caf50;color:#fff;padding:8px;margin:-12px -12px 8px -12px;border-radius:4px 4px 0 0;">
          <strong>📍 KONUMUNUZ</strong>
        </div>
        <strong>Mevcut Konum</strong><br/>
        <em>${location.lat.toFixed(6)}, ${location.lon.toFixed(6)}</em>
      </div>
    `);

    // Haritayı kullanıcı konumuna zoom yap
    map.current.setView([location.lat, location.lon], 13);
  };

  // Real-time konum takibi başlat
  const startRealTimeTracking = (seq: { d: Delivery; pt: GeoPt }[]) => {
    if (!navigator.geolocation || seq.length === 0) return;
    
    // Eski watch'ı temizle
    if (watchId.current !== null) {
      navigator.geolocation.clearWatch(watchId.current);
    }

    setIsTrackingLocation(true);
    console.log('Real-time konum takibi başlatıldı');

    watchId.current = navigator.geolocation.watchPosition(
      (position) => {
        const user: GeoPt = { 
          lat: position.coords.latitude, 
          lon: position.coords.longitude 
        };
        setUserLocation(user);
        
        // Hız takibi
        trackSpeed(position);
        
        // Kullanıcı konumunu haritada güncelle
        if (isNavigating) {
          // Navigasyon modunda özel marker kullan
          if (userLocationMarkerRef.current) {
            map.current.removeLayer(userLocationMarkerRef.current);
          }
          const L = (window as any).L;
          userLocationMarkerRef.current = L.marker([user.lat, user.lon], {
            icon: new L.DivIcon({
              html: '<div style="background:#4285f4;color:#fff;border-radius:50%;width:40px;height:40px;display:flex;align-items:center;justify-content:center;font-weight:bold;border:4px solid #fff;box-shadow:0 4px 12px rgba(0,0,0,0.3);">🚗</div>',
              className: 'navigation-marker'
            })
          }).addTo(map.current);
        } else {
          showUserLocationOnMap(user);
        }
        
        // Sıradaki durağa olan mesafeyi hesapla - GERÇEK KONUMA GÖRE
        const first = seq[0];
        if (first) {
          // Her konum güncellemesinde gerçek mesafeyi hesapla
          const distanceKm = haversine(user, first.pt);
          setCurrentDistanceToNext(distanceKm);
          
          // 30 saniyede bir OSRM ile gerçek sürüş mesafesi al
          const now = Date.now();
          if (now - lastLegCalcRef.current > 30000) {
            lastLegCalcRef.current = now;
            updateRealTimeDistance(user, first.pt);
          }
        }
      },
      (error) => {
        console.error('Konum takibi hatası:', error);
        setIsTrackingLocation(false);
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 5000 // 5 saniye
      }
    );
  };

  // Real-time mesafe güncelleme
  const updateRealTimeDistance = async (from: GeoPt, to: GeoPt) => {
    try {
      const url = `${API_BASE}/utils/osrm?coords=${from.lon},${from.lat};${to.lon},${to.lat}`;
      const response = await fetch(url);
      const data = await response.json();
      const route = data?.routes?.[0];
      
      if (route) {
        setCurrentDistanceToNext((route.distance || 0) / 1000);
        setCurrentDurationToNext((route.duration || 0) / 60);
        setNextDriveKm((route.distance || 0) / 1000);
        setNextDriveMin((route.duration || 0) / 60);
      }
    } catch (error) {
      console.warn('Real-time mesafe güncellenemedi:', error);
      // Fallback: haversine mesafesi kullan
      const distanceKm = haversine(from, to);
      setCurrentDistanceToNext(distanceKm);
      setCurrentDurationToNext(distanceKm * 1.5); // Basit tahmin: 40 km/h
    }
  };

  // Sürüş modunu başlat
  const startDrivingMode = async (userLocation: GeoPt, firstStop: GeoPt) => {
    if (!(window as any).L || !map.current) return;
    const L = (window as any).L;

    console.log('Sürüş modu başlatılıyor...');
    setIsDrivingMode(true);

    try {
      // Kullanıcı konumundan ilk durağa rota al
      const coordStr = `${userLocation.lon},${userLocation.lat};${firstStop.lon},${firstStop.lat}`;
      const url = `${API_BASE}/utils/mroute?coords=${coordStr}`;
      const response = await fetch(url);
      const data = await response.json();
      const route = data?.routes?.[0];

      if (route && route.geometry) {
        // Sürüş rotasını çiz
        const coordinates = route.geometry.coordinates.map((c: [number, number]) => [c[1], c[0]]);
        const drivingPolyline = L.polyline(coordinates, {
          color: '#ff5722',
          weight: 6,
          opacity: 0.8,
          dashArray: '10, 5'
        }).addTo(map.current);

        setDrivingRoute(drivingPolyline);

        // Haritayı sürüş rotasına odakla
        map.current.fitBounds(coordinates, { padding: [20, 20] });

        // Mesafe ve süre bilgilerini güncelle
        setCurrentDistanceToNext((route.distance || 0) / 1000);
        setCurrentDurationToNext((route.duration || 0) / 60);
        setNextDriveKm((route.distance || 0) / 1000);
        setNextDriveMin((route.duration || 0) / 60);

        console.log('Sürüş modu başlatıldı - Mesafe:', (route.distance || 0) / 1000, 'km');
      } else {
        // Fallback: düz çizgi
        console.warn('Sürüş rotası alınamadı, düz çizgi çiziliyor');
        const fallbackRoute = L.polyline([[userLocation.lat, userLocation.lon], [firstStop.lat, firstStop.lon]], {
          color: '#ff5722',
          weight: 4,
          opacity: 0.6,
          dashArray: '15, 10'
        }).addTo(map.current);
        setDrivingRoute(fallbackRoute);
      }
    } catch (error) {
      console.error('Sürüş modu başlatılamadı:', error);
      setIsDrivingMode(false);
    }
  };

  // Sürüş modunu durdur
  const stopDrivingMode = () => {
    if (drivingRoute && map.current) {
      map.current.removeLayer(drivingRoute);
      setDrivingRoute(null);
    }
    setIsDrivingMode(false);
    setIsNavigating(false);
    setNextInstruction('');
    setDistanceToNextTurn(null);
    setCurrentSpeed(null);
    setNavigationStep(0);
    
    // Navigasyon interval'ini temizle
    if (navigationIntervalRef.current) {
      clearInterval(navigationIntervalRef.current);
      navigationIntervalRef.current = null;
    }
    
    console.log('Sürüş modu durduruldu');
  };

  // Google Maps benzeri navigasyon başlat
  const startNavigation = (userLocation: GeoPt, destination: GeoPt) => {
    if (!(window as any).L || !map.current) return;
    const L = (window as any).L;

    console.log('Navigasyon başlatılıyor...');
    setIsNavigating(true);
    setNavigationStep(0);
    setNextInstruction('Rotaya başlanıyor...');

    // Haritayı navigasyon moduna geçir
    map.current.setView([userLocation.lat, userLocation.lon], 16);
    
    // Kullanıcı konumunu navigasyon stili ile göster
    if (userLocationMarkerRef.current) {
      map.current.removeLayer(userLocationMarkerRef.current);
    }
    
    userLocationMarkerRef.current = L.marker([userLocation.lat, userLocation.lon], {
      icon: new L.DivIcon({
        html: '<div style="background:#4285f4;color:#fff;border-radius:50%;width:40px;height:40px;display:flex;align-items:center;justify-content:center;font-weight:bold;border:4px solid #fff;box-shadow:0 4px 12px rgba(0,0,0,0.3);">🚗</div>',
        className: 'navigation-marker'
      })
    }).addTo(map.current);

    // Navigasyon talimatlarını simüle et
    simulateNavigationInstructions(userLocation, destination);
  };

  // Navigasyon talimatlarını simüle et
  const simulateNavigationInstructions = (start: GeoPt, end: GeoPt) => {
    const instructions = [
      'Rotaya başlanıyor...',
      'İleri doğru devam edin',
      '200m sonra sağa dönün',
      'Ana yola çıkın',
      '500m sonra sola dönün',
      'Hedefinize yaklaşıyorsunuz',
      '100m sonra varış noktasına ulaşacaksınız'
    ];

    let step = 0;
    navigationIntervalRef.current = setInterval(() => {
      if (step < instructions.length) {
        setNextInstruction(instructions[step]);
        setNavigationStep(step);
        
        // Mesafe simülasyonu KALDIRILDI - gerçek konuma göre hesaplanacak
        // setDistanceToNextTurn(remainingDistance);
        
        step++;
      } else {
        // Navigasyon tamamlandı
        setNextInstruction('Hedefinize ulaştınız!');
        setIsNavigating(false);
        if (navigationIntervalRef.current) {
          clearInterval(navigationIntervalRef.current);
          navigationIntervalRef.current = null;
        }
      }
    }, 3000); // Her 3 saniyede bir talimat güncelle
  };

  // Hız takibi
  const trackSpeed = (position: GeolocationPosition) => {
    if (position.coords.speed !== null) {
      setCurrentSpeed(Math.round(position.coords.speed * 3.6)); // m/s to km/h
    }
  };

  // Mapbox GL JS ile gerçek navigasyon başlat
  const startMapboxNavigation = async (userLocation: GeoPt, destination: GeoPt) => {
    if (!map.current) return;
    
    try {
      // Mapbox GL JS'i dynamic import ile yükle
      const mapboxgl = await import('mapbox-gl');
      
      // Mapbox GL JS'i başlat
      mapboxgl.default.accessToken = MAPBOX_CONFIG.ACCESS_TOKEN;
      
      // Mevcut Leaflet map'i temizle
      map.current.remove();
      
      // Mapbox GL JS map oluştur
      const mapboxMap = new mapboxgl.default.Map({
        container: 'map',
        style: MAPBOX_CONFIG.NAVIGATION_STYLE,
        center: [userLocation.lon, userLocation.lat],
        zoom: 15
      });
      
      // Kullanıcı konumu marker'ı
      const userMarker = new mapboxgl.default.Marker({
        color: '#4285f4',
        scale: 1.2
      })
        .setLngLat([userLocation.lon, userLocation.lat])
        .addTo(mapboxMap);
      
      // Hedef marker'ı
      const destMarker = new mapboxgl.default.Marker({
        color: '#ff5722',
        scale: 1.2
      })
        .setLngLat([destination.lon, destination.lat])
        .addTo(mapboxMap);
      
      // Backend üzerinden Mapbox Directions API çağrısı
      const response = await fetch(
        `${API_BASE}/utils/mapbox-directions?origin=${userLocation.lon},${userLocation.lat}&destination=${destination.lon},${destination.lat}`
      );
      
      const result = await response.json();
      
      if (result.success && result.data.routes && result.data.routes.length > 0) {
        const route = result.data.routes[0];
        
        // Rota çiz
        mapboxMap.addSource('route', {
          type: 'geojson',
          data: {
            type: 'Feature',
            properties: {},
            geometry: {
              type: 'LineString',
              coordinates: route.geometry.coordinates
            }
          }
        });
        
        mapboxMap.addLayer({
          id: 'route',
          type: 'line',
          source: 'route',
          layout: {
            'line-join': 'round',
            'line-cap': 'round'
          },
          paint: {
            'line-color': '#4285f4',
            'line-width': 6
          }
        });
        
        // Navigasyon talimatları
        if (route.legs && route.legs[0].steps) {
          const steps = route.legs[0].steps;
          setNextInstruction(steps[0].maneuver.instruction || 'Rotaya başlanıyor...');
        }
        
        // Mesafe ve süre güncelle
        setCurrentDistanceToNext(route.distance / 1000); // km'ye çevir
        setCurrentDurationToNext(Math.round(route.duration / 60)); // dakikaya çevir
        
        console.log('Mapbox navigasyon başlatıldı:', route);
      }
      
    } catch (error) {
      console.error('Mapbox navigasyon hatası:', error);
    }
  };

  useEffect(() => {
    const load = async () => {
      try {
        setError('');
        setLoading(true);
        await loadLeaflet();
        // Ayarlardan varsayılanları yükle (opsiyonel hafıza)
        // Backend: supplier address'i al (geçiş için localStorage fallback)
        try {
          const mySupplier = await apiService.getMySupplier();
          if (mySupplier?.address) setSupplierAddress(mySupplier.address);
        } catch {}
        const saved = JSON.parse(localStorage.getItem('supplier_settings') || '{}');
        if (saved?.routeStart) setRouteStart(saved.routeStart);
        if (saved?.useAirline !== undefined) setUseAirline(saved.useAirline);
        
        // Önce aktif plan varsa yükle
        try {
          const plan = await apiService.getActiveRoutePlan();
          if (plan) {
            const p = JSON.parse(plan);
            if (p?.routeStart) setRouteStart(p.routeStart);
            if (p?.supplierAddress) setSupplierAddress(p.supplierAddress);
            if (p?.useAirline !== undefined) setUseAirline(p.useAirline);
          }
        } catch {}

        // Admin kullanıcıları için tüm teslimatları, supplier için kendi teslimatlarını yükle
        const response = isAdmin 
          ? await apiService.getAllDeliveries(0, 100) // Admin için tüm teslimatlar
          : await apiService.getMyDeliveries(); // Supplier için kendi teslimatları
        
        setDeliveries(response.content || []);
      } catch (e: any) {
        setError(e.response?.data?.message || 'Teslimatlar yüklenemedi');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [isAdmin]);

  // Cleanup: Component unmount olduğunda konum takibini durdur
  useEffect(() => {
    return () => {
      if (watchId.current !== null) {
        navigator.geolocation.clearWatch(watchId.current);
      }
      if (navigationIntervalRef.current) {
        clearInterval(navigationIntervalRef.current);
      }
    };
  }, []);

  // Değişiklikleri hafızaya yaz (sessiz)
  useEffect(() => {
    const current = JSON.parse(localStorage.getItem('supplier_settings') || '{}');
    localStorage.setItem('supplier_settings', JSON.stringify({
      ...current,
      routeStart,
      supplierAddress,
      useAirline,
    }));
    // Adres değişirse backend'e kaydet
    if (supplierAddress && supplierAddress.length > 5) {
      apiService.updateMySupplier({ address: supplierAddress }).catch(() => {});
    }
  }, [routeStart, supplierAddress, useAirline]);

  const ordered = useMemo(() => deliveries.filter(d => 
    d.deliveryStatus !== 'DELIVERED' && (
      (d.marketLat && d.marketLng) || // Koordinat varsa kesin kullan
      !!d.marketAddress || !!d.marketName // Yoksa adres varsa geocoding dene
    )
  ), [deliveries]);

  useEffect(() => {
    const initMapAndRoute = async () => {
      if (!mapRef.current || !(window as any).L) return;
      const L = (window as any).L;
      if (!map.current) {
        map.current = L.map(mapRef.current).setView([41.015137, 28.97953], 12);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          attribution: '&copy; OpenStreetMap contributors',
        }).addTo(map.current);
      }

      // Get coordinates (prioritize DB coordinates, fallback to geocoding)
      const points: { d: Delivery; pt: GeoPt }[] = [];
      for (const d of ordered) {
        let pt: GeoPt | null = null;
        
        // Önce DB koordinatlarını kontrol et
        if (d.marketLat && d.marketLng) {
          pt = { lat: d.marketLat, lon: d.marketLng };
          console.log(`Using DB coordinates for ${d.marketName}: ${d.marketLat}, ${d.marketLng}`);
        } else {
          // Koordinat yoksa geocoding dene
          const addr = d.marketAddress || d.marketName;
          if (addr) {
            pt = await geocodeAddress(addr);
            console.log(`Geocoded ${addr} to:`, pt);
          }
        }
        
        if (pt) {
          points.push({ d, pt });
        } else {
          console.warn(`No coordinates found for delivery ${d.id} - ${d.marketName}`);
        }
      }
      if (points.length === 0) return;

      // NN order from current center
      const center = map.current.getCenter();
      let current: GeoPt = { lat: center.lat, lon: center.lng };
      const remaining = [...points];
      const seq: { d: Delivery; pt: GeoPt }[] = [];
      while (remaining.length) {
        remaining.sort((a, b) => haversine(current, a.pt) - haversine(current, b.pt));
        const nxt = remaining.shift()!;
        seq.push(nxt);
        current = nxt.pt;
      }
      setSequence(seq);

      // draw initial
      drawRoute(seq);
      startProximityWatch(seq);
    };

    initMapAndRoute();

    return () => {
      if (watchId.current !== null && navigator.geolocation) {
        navigator.geolocation.clearWatch(watchId.current);
        watchId.current = null;
      }
    };
  }, [ordered]);

  const drawRoute = (seq: { d: Delivery; pt: GeoPt }[]) => {
    if (!(window as any).L || !map.current) return;
    const L = (window as any).L;

    // clear
    markersRef.current.forEach((m) => map.current.removeLayer(m));
    markersRef.current = [];
    if (polylineRef.current) {
      map.current.removeLayer(polylineRef.current);
      polylineRef.current = null;
    }
    // Sürüş modu rotasını da temizle
    if (drivingRoute) {
      map.current.removeLayer(drivingRoute);
      setDrivingRoute(null);
      setIsDrivingMode(false);
    }

    const poly: any[] = [];
    seq.forEach((item, idx) => {
      const m = L.marker([item.pt.lat, item.pt.lon]).addTo(map.current);
      
      // Sıradaki durak (ilk) özel ikonla vurgula
      if (idx === 0) {
        m.setIcon(new L.DivIcon({ 
          html: '<div style="background:#ff5722;color:#fff;border-radius:50%;width:36px;height:36px;display:flex;align-items:center;justify-content:center;font-weight:bold;border:3px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,0.3);">📍</div>',
          className: 'next-stop-marker'
        }));
      } else {
        // Diğer duraklar için standart numaralı marker
        m.setIcon(new L.DivIcon({ 
          html: `<div style="background:#1976d2;color:#fff;border-radius:50%;width:28px;height:28px;display:flex;align-items:center;justify-content:center;font-weight:bold;border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,0.2);">${idx + 1}</div>`,
          className: 'delivery-marker'
        }));
      }
      
        // Popup içeriği - sıradaki durak için farklı stil
        const isNext = idx === 0;
        const showCompleteButton = isSupplier; // Sadece supplier kullanıcıları teslimat tamamlayabilir
        const isDelivered = item.d.deliveryStatus === 'DELIVERED';
        
        const popupContent = isNext 
          ? `<div style="min-width:200px;text-align:center;">
               <div style="background:#ff5722;color:#fff;padding:8px;margin:-12px -12px 8px -12px;border-radius:4px 4px 0 0;">
                 <strong>🚚 SIRADAKI DURAK</strong>
               </div>
               <strong style="font-size:16px;">${item.d.marketName}</strong><br/>
               <em>${item.d.marketAddress || ''}</em><br/>
               ${showCompleteButton && !isDelivered ? `<button id="complete-${item.d.id}" style="background:#4caf50;color:#fff;border:none;padding:8px 16px;border-radius:4px;margin-top:8px;cursor:pointer;font-weight:bold;">📦 Teslim Et</button>` : ''}
               ${isDelivered ? `<div style="background:#e8f5e8;color:#2e7d32;padding:8px;border-radius:4px;margin-top:8px;font-weight:bold;">✅ Teslim Edildi</div>` : ''}
             </div>`
          : `<div style="min-width:180px;">
               <strong>${idx + 1}. ${item.d.marketName}</strong><br/>
               <em>${item.d.marketAddress || ''}</em><br/>
               ${showCompleteButton && !isDelivered ? `<button id="complete-${item.d.id}" style="background:#2196f3;color:#fff;border:none;padding:6px 12px;border-radius:4px;margin-top:6px;cursor:pointer;">📦 Teslim Et</button>` : ''}
               ${isDelivered ? `<div style="background:#e8f5e8;color:#2e7d32;padding:6px;border-radius:4px;margin-top:6px;font-weight:bold;">✅ Teslim Edildi</div>` : ''}
             </div>`;
      
      m.bindPopup(popupContent);
      m.on('popupopen', () => {
        const btn = document.getElementById(`complete-${item.d.id}`);
        if (btn && isSupplier && !isDelivered) { // Sadece supplier kullanıcıları ve teslim edilmemiş için buton işlevi
          btn.onclick = async () => {
            try {
              // Buton disable et ve loading göster
              btn.innerHTML = '⏳ Teslim ediliyor...';
              btn.style.opacity = '0.6';
              (btn as HTMLButtonElement).disabled = true;
              
              await apiService.completeDelivery(item.d.id);
              
              // Başarılı mesaj göster - popup'ı güncelle
              const popup = m.getPopup();
              const updatedContent = isNext 
                ? `<div style="min-width:200px;text-align:center;">
                     <div style="background:#ff5722;color:#fff;padding:8px;margin:-12px -12px 8px -12px;border-radius:4px 4px 0 0;">
                       <strong>🚚 SIRADAKI DURAK</strong>
                     </div>
                     <strong style="font-size:16px;">${item.d.marketName}</strong><br/>
                     <em>${item.d.marketAddress || ''}</em><br/>
                     <div style="background:#e8f5e8;color:#2e7d32;padding:8px;border-radius:4px;margin-top:8px;font-weight:bold;">✅ Teslim Edildi</div>
                   </div>`
                : `<div style="min-width:180px;">
                     <strong>${idx + 1}. ${item.d.marketName}</strong><br/>
                     <em>${item.d.marketAddress || ''}</em><br/>
                     <div style="background:#e8f5e8;color:#2e7d32;padding:6px;border-radius:4px;margin-top:6px;font-weight:bold;">✅ Teslim Edildi</div>
                   </div>`;
              
              popup.setContent(updatedContent);
              
              // 2 saniye sonra listeden çıkar ve yeniden çiz
              setTimeout(() => {
                const nextSeq = seq.filter((x) => x.d.id !== item.d.id);
                setSequence(nextSeq);
                if (nextSeq.length > 0) {
                  drawRoute(nextSeq);
                  startProximityWatch(nextSeq); // Yeni sıradaki durak için proximity watch başlat
                } else {
                  // Tüm teslimatlar tamamlandı
                  map.current.closePopup();
                  const L = (window as any).L;
                  L.popup()
                    .setLatLng([item.pt.lat, item.pt.lon])
                    .setContent('<div style="text-align:center;padding:10px;"><strong>🎉 Tüm teslimatlar tamamlandı!</strong><br/>Harika iş çıkardınız!</div>')
                    .openOn(map.current);
                }
              }, 2000);
              
            } catch (error) {
              // Hata durumunda geri al
              btn.innerHTML = '❌ Hata! Tekrar dene';
              btn.style.background = '#f44336';
              btn.style.opacity = '1';
              (btn as HTMLButtonElement).disabled = false;
              console.error('Delivery completion failed:', error);
            }
          };
        }
      });
      markersRef.current.push(m);
      poly.push([item.pt.lat, item.pt.lon]);
    });

    // Sadece yol rotası çiz (kuş uçuşu seçeneğini kaldır)
    const drawOsrm = async () => {
      try {
        // Başlangıç noktasını belirle
        let startPt: GeoPt | null = null;
        if (routeStart === 'mylocation' && userLocation) {
          startPt = userLocation;
        } else if (routeStart === 'address' && supplierAddress) {
          try { startPt = await geocodeAddress(supplierAddress); } catch {}
        }

        const allPts: GeoPt[] = [];
        if (startPt) allPts.push(startPt);
        seq.forEach((s) => allPts.push(s.pt));
        if (allPts.length < 2) throw new Error('Not enough points');

        // Çoklu durak endpoint'i ile tek çağrıda rota al
        const coordStr = allPts.map(p => `${p.lon},${p.lat}`).join(';');
        const url = `${API_BASE}/utils/mroute?coords=${coordStr}`;
        const res = await fetch(url);
        const data = await res.json();
        const route = data?.routes?.[0];
        if (route && route.geometry) {
          const stitched = route.geometry.coordinates.map((c: [number, number]) => [c[1], c[0]]);
          const L2 = (window as any).L;
          polylineRef.current = L2.polyline(stitched, { color: 'blue' }).addTo(map.current);
          map.current.fitBounds(stitched);
          setTotalDistanceKm(((route.distance || 0) / 1000) || null);
          setTotalDurationMin(((route.duration || 0) / 60) || null);
        } else {
          // Rota bulunamadı - basit düz çizgi çiz
          console.warn('Yol rotası bulunamadı, basit rota çiziliyor');
          const L2 = (window as any).L;
          polylineRef.current = L2.polyline(poly, { color: 'orange', dashArray: '5, 5' }).addTo(map.current);
          map.current.fitBounds(poly);
          
          // Basit mesafe hesapla
          let totalKm = 0;
          for (let i = 1; i < poly.length; i++) {
            const a: GeoPt = { lat: poly[i - 1][0], lon: poly[i - 1][1] } as any;
            const b: GeoPt = { lat: poly[i][0], lon: poly[i][1] } as any;
            totalKm += haversine(a, b);
          }
          setTotalDistanceKm(totalKm);
          setTotalDurationMin(null);
        }
      } catch (e) {
        // Rota hesaplanamadı - basit düz çizgi çiz
        console.error('Rota hesaplanamadı:', e);
        const L2 = (window as any).L;
        polylineRef.current = L2.polyline(poly, { color: 'red', dashArray: '10, 10' }).addTo(map.current);
        map.current.fitBounds(poly);
        
        // Basit mesafe hesapla
        let totalKm = 0;
        for (let i = 1; i < poly.length; i++) {
          const a: GeoPt = { lat: poly[i - 1][0], lon: poly[i - 1][1] } as any;
          const b: GeoPt = { lat: poly[i][0], lon: poly[i][1] } as any;
          totalKm += haversine(a, b);
        }
        setTotalDistanceKm(totalKm);
        setTotalDurationMin(null);
      }
    };

      // Her zaman yol rotası çiz
      drawOsrm();
      
      // Real-time tracking başlat (eğer konum alınmışsa)
      if (userLocation) {
        startRealTimeTracking(seq);
      }
    };

  // Teslim edilmiş durakları gri pinlerle göster (rota dışında)
  useEffect(() => {
    if (!(window as any).L || !map.current) return;
    const L = (window as any).L;
    
    // Önce eski teslim edilmiş marker'ları temizle
    const existingDeliveredMarkers = document.querySelectorAll('.delivered-marker');
    existingDeliveredMarkers.forEach(marker => {
      const leafletMarker = (marker as any)._leaflet_id;
      if (leafletMarker && map.current._layers[leafletMarker]) {
        map.current.removeLayer(map.current._layers[leafletMarker]);
      }
    });
    
    deliveries
      .filter(d => d.deliveryStatus === 'DELIVERED')
      .forEach(d => {
        const lat = d.marketLat;
        const lon = d.marketLng;
        if (lat && lon) {
          const marker = L.marker([lat, lon]).addTo(map.current);
          marker.setIcon(new L.DivIcon({
            html: '<div style="background:#9e9e9e;color:#fff;border-radius:50%;width:24px;height:24px;display:flex;align-items:center;justify-content:center;border:2px solid #fff;">✓</div>',
            className: 'delivered-marker'
          }));
          marker.bindPopup(`<div style="min-width:160px;"><strong>${d.marketName}</strong><br/><em>✅ Teslim edildi</em></div>`);
        }
      });
  }, [deliveries]);

  const startProximityWatch = (seq: { d: Delivery; pt: GeoPt }[]) => {
    // Real-time tracking kullan
    startRealTimeTracking(seq);
  };

  const openGoogleRoute = () => {
    const start = routeStart === 'mylocation' ? 'Current+Location' : encodeURIComponent(supplierAddress || '');
    const stops = deliveries
      .filter(d => d.deliveryStatus !== 'DELIVERED')
      .map(d => encodeURIComponent(d.marketAddress || d.marketName));
    const url = start ? `https://www.google.com/maps/dir/${start}/${stops.join('/')}` : `https://www.google.com/maps/dir/${stops.join('/')}`;
    window.open(url, '_blank');
    // Plan snapshot kaydet
    try {
      const planJson = JSON.stringify({ routeStart, supplierAddress, useAirline, stops: deliveries.filter(d=>d.deliveryStatus!=='DELIVERED').map(d=>({id:d.id, addr:d.marketAddress||d.marketName})) });
      apiService.saveRoutePlan(planJson).catch(()=>{});
    } catch {}
  };

  // Rol kontrolü - Admin veya Supplier olmalı
  if (!isAdmin && !isSupplier) {
    return (
      <Box sx={{ p: { xs: 2, md: 3 } }}>
        <Alert severity="error" sx={{ mb: 2 }}>
          Access denied. Admin or Supplier role required.
        </Alert>
        <Button 
          variant="contained" 
          onClick={() => window.open('https://www.google.com/maps/dir/', '_blank')}
        >
          GOOGLE ROTA
        </Button>
      </Box>
    );
  }

  return (
    <Box sx={{ p: { xs: 2, md: 3 } }}>
      <Typography component="h1" sx={{ fontSize: { xs: '1.125rem', md: '2.125rem' }, fontWeight: 700 }} gutterBottom>
        Harita ve Rota
      </Typography>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        
        {isDrivingMode && (
          <Alert severity="info" sx={{ mb: 2, backgroundColor: '#e3f2fd' }}>
            <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
              🚗 Sürüş Modu Aktif - Konumunuzdan ilk durağa giden rota çizildi
            </Typography>
            <Typography variant="caption" display="block" sx={{ mt: 0.5 }}>
              Real-time konum takibi ile mesafe ve süre bilgileri güncelleniyor
            </Typography>
          </Alert>
        )}
        
        {isNavigating && (
          <Alert severity="success" sx={{ mb: 2, backgroundColor: '#e8f5e8' }}>
            <Typography variant="body2" sx={{ fontWeight: 'bold', color: '#2e7d32' }}>
              🧭 Navigasyon Aktif - Google Maps benzeri yönlendirme
            </Typography>
            <Typography variant="caption" display="block" sx={{ mt: 0.5, color: '#2e7d32' }}>
              {nextInstruction}
            </Typography>
          </Alert>
        )}
      
      {/* Google Maps Benzeri Navigasyon Paneli */}
      {isNavigating && (
        <Paper sx={{ p: { xs: 2, md: 3 }, mb: 2, background: 'linear-gradient(135deg, #4285f4 0%, #1976d2 100%)', color: 'white' }}>
          <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', fontWeight: 'bold' }}>
            🧭 Navigasyon Aktif
          </Typography>
          
          <Stack direction="row" spacing={4} flexWrap="wrap" rowGap={2}>
            <Box>
              <Typography variant="body2" sx={{ opacity: 0.9 }}>Mevcut Hız</Typography>
              <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                {currentSpeed ? `${currentSpeed} km/h` : '-- km/h'}
              </Typography>
            </Box>
            
            <Box>
              <Typography variant="body2" sx={{ opacity: 0.9 }}>Sıradaki Talimat</Typography>
              <Typography variant="h6" sx={{ fontWeight: 'bold', maxWidth: 300 }}>
                {nextInstruction}
              </Typography>
            </Box>
            
            <Box>
              <Typography variant="body2" sx={{ opacity: 0.9 }}>Kalan Mesafe</Typography>
              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                {currentDistanceToNext ? `${currentDistanceToNext.toFixed(1)} km` : '-- km'}
              </Typography>
            </Box>
            
            <Box>
              <Typography variant="body2" sx={{ opacity: 0.9 }}>Navigasyon Adımı</Typography>
              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                {navigationStep + 1}/7
              </Typography>
            </Box>
          </Stack>
        </Paper>
      )}
      
      {/* Teslimat Durum Paneli */}
      {sequence.length > 0 && (
        <Paper sx={{ p: { xs: 2, md: 3 }, mb: 2, background: 'linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%)' }}>
          <Typography variant="subtitle1" sx={{ mb: 1, display: 'flex', alignItems: 'center', fontWeight: 700 }}>
            🚚 Teslimat Bilgileri
          </Typography>
          <Stack direction="row" spacing={4} flexWrap="wrap" rowGap={2}>
            <Box>
              <Typography variant="body2" color="textSecondary">Kalan Teslimat</Typography>
              <Typography variant="h6" color="primary">{sequence.length} durak</Typography>
            </Box>
            {sequence[0] && (
              <Box>
                <Typography variant="body2" color="textSecondary">
                  Sıradaki Durağa Mesafe
                  {isTrackingLocation && (
                    <span style={{ color: '#4caf50', marginLeft: '8px' }}>🟢</span>
                  )}
                  {isDrivingMode && (
                    <span style={{ color: '#ff5722', marginLeft: '8px' }}>🚗</span>
                  )}
                </Typography>
                <Typography variant="h6" color="error">
                  {currentDistanceToNext ? `${currentDistanceToNext.toFixed(1)} km` : 
                   nextDriveKm != null ? `${nextDriveKm.toFixed(1)} km` : 
                   (userLocation ? `${(haversine(userLocation, sequence[0].pt)).toFixed(1)} km (kuş uçuşu)` : '...')}
                </Typography>
                {(currentDurationToNext !== null || nextDriveMin != null) && (
                  <Typography variant="body2" color="textSecondary">
                    ~ {Math.round(currentDurationToNext || nextDriveMin || 0)} dk
                    {isTrackingLocation && ' (real-time)'}
                    {isDrivingMode && ' (sürüş modu)'}
                  </Typography>
                )}
              </Box>
            )}
            {(totalDistanceKm != null) && (
              <Box>
                <Typography variant="body2" color="textSecondary">Toplam Rota Mesafesi</Typography>
                <Typography variant="h6">{totalDistanceKm.toFixed(1)} km</Typography>
                {totalDurationMin != null && (
                  <Typography variant="body2" color="textSecondary">~ {Math.round(totalDurationMin)} dk</Typography>
                )}
              </Box>
            )}
            <Box>
              <Typography variant="body2" color="textSecondary">Sıradaki Durak</Typography>
              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                📍 {sequence[0]?.d.marketName || 'Yükleniyor...'}
              </Typography>
            </Box>
          </Stack>
          <Stack direction="row" spacing={2} sx={{ mt: 2 }} flexWrap="wrap" rowGap={1}>
            <Button
              size="small"
              variant="contained"
              onClick={() => {
                setRouteStart('mylocation');
                getCurrentLocation();
              }}
              disabled={isLoadingLocation}
              sx={{ minWidth: 200 }}
              color={isTrackingLocation ? 'success' : 'primary'}
            >
              {isLoadingLocation ? '⏳ Konum alınıyor...' : 
               isTrackingLocation ? '🟢 Konum Takibi Aktif' : '📍 Konumumdan Başlat'}
            </Button>
            
            {isTrackingLocation && (
              <Button
                size="small"
                variant="outlined"
                onClick={() => {
                  if (watchId.current !== null) {
                    navigator.geolocation.clearWatch(watchId.current);
                    watchId.current = null;
                  }
                  setIsTrackingLocation(false);
                  setCurrentDistanceToNext(null);
                  setCurrentDurationToNext(null);
                  stopDrivingMode();
                }}
                color="error"
                sx={{ minWidth: 150 }}
              >
                🛑 Takibi Durdur
              </Button>
            )}
            
            {isDrivingMode && (
              <Button
                size="small"
                variant="contained"
                onClick={stopDrivingMode}
                color="warning"
                sx={{ minWidth: 150 }}
              >
                🚗 Sürüş Modunu Durdur
              </Button>
            )}
            
      {isNavigating && (
        <Button
          size="small"
          variant="contained"
          onClick={() => {
            setIsNavigating(false);
            if (navigationIntervalRef.current) {
              clearInterval(navigationIntervalRef.current);
              navigationIntervalRef.current = null;
            }
          }}
          color="error"
          sx={{ minWidth: 150 }}
        >
          🛑 Navigasyonu Durdur
        </Button>
      )}
      
      <Button
        size="small"
        variant="contained"
        onClick={() => {
          if (userLocation && sequence.length > 0) {
            startMapboxNavigation(userLocation, sequence[0].pt);
          }
        }}
        color="info"
        sx={{ minWidth: 150 }}
        disabled={!userLocation || sequence.length === 0}
      >
        🗺️ Mapbox Navigasyon
      </Button>
          </Stack>
          <Stack direction="row" spacing={1} sx={{ mt: 2 }} flexWrap="wrap" rowGap={1}>
            <Button size="small" variant="contained" onClick={() => drawRoute(sequence)}>
              🛣️ Yol Rotası Güncelle
            </Button>
          </Stack>
          {userLocation && sequence[0] && haversine(userLocation, sequence[0].pt) * 1000 < 150 && (
            <Alert severity="info" sx={{ mt: 2 }}>
              🎯 Hedefe yaklaştınız! Teslim etmeye hazır olun.
            </Alert>
          )}
        </Paper>
      )}
      
      <Stack direction="row" spacing={1} sx={{ mb: 2 }} flexWrap="wrap" rowGap={1}>
        <Button size="small" variant="contained" onClick={openGoogleRoute}>Google Rota</Button>
        {sequence.length > 0 && (
          <Button 
            size="small" 
            variant="outlined" 
            onClick={() => {
              if (sequence[0]) {
                markersRef.current[0]?.openPopup();
              }
            }}
          >
            Sıradaki Durağı Göster
          </Button>
        )}
      </Stack>
      
      <Paper sx={{ height: { xs: 420, md: 520 }, mb: 2 }}>
        <div ref={mapRef} style={{ height: '100%', width: '100%' }} />
      </Paper>
      
      {/* Teslimat Listesi */}
      {sequence.length > 0 && (
        <Paper sx={{ p: { xs: 2, md: 3 } }}>
          <Typography variant="h6" gutterBottom>Teslimat Sırası</Typography>
          {sequence.map((item, idx) => (
            <Box 
              key={item.d.id} 
              sx={{ 
                p: 1, 
                mb: 1, 
                border: idx === 0 ? '2px solid #ff5722' : '1px solid #ddd',
                borderRadius: 1,
                background: idx === 0 ? '#fff3e0' : '#fff'
              }}
            >
              <Stack direction="row" alignItems="center" spacing={2}>
                <Typography 
                  variant="body2" 
                  sx={{ 
                    minWidth: 24, 
                    height: 24, 
                    borderRadius: '50%', 
                    background: idx === 0 ? '#ff5722' : '#1976d2', 
                    color: '#fff',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '12px',
                    fontWeight: 'bold'
                  }}
                >
                  {idx === 0 ? '📍' : idx + 1}
                </Typography>
                <Box sx={{ flex: 1 }}>
                  <Typography variant="body1" fontWeight={idx === 0 ? 'bold' : 'normal'}>
                    {item.d.marketName}
                  </Typography>
                  <Typography variant="body2" color="textSecondary">
                    {item.d.marketAddress}
                  </Typography>
                </Box>
                {userLocation && (
                  <Typography variant="body2" color="textSecondary">
                    {(haversine(userLocation, item.pt)).toFixed(1)} km
                  </Typography>
                )}
              </Stack>
            </Box>
          ))}
        </Paper>
      )}
    </Box>
  );
};

export default DeliveriesMapPage;


