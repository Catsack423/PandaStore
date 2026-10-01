/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'images.unsplash.com',
      },
    ],
  },
  allowedDevOrigins: [
    '172.27.0.1',
    'localhost:3000',
    '172.27.0.1:3000',
  ],
};

module.exports = nextConfig;
