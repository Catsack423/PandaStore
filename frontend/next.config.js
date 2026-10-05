/** @type {import('next').NextConfig} */
const nextConfig = {
  allowedDevOrigins: [
    '172.27.0.1',
    'localhost:3000',
    '172.27.0.1:3000',
  ],
};

module.exports = nextConfig;
