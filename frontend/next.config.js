/** @type {import('next').NextConfig} */
const nextConfig = {
  // สำหรับ Next.js 14+ / 15
  experimental: {
    allowedDevOrigins: [
      '172.27.0.1',
      'localhost:3000',
      '172.27.0.1:3000', // ใส่ port ที่ใช้งานด้วยหากมีการระบุ port
    ],
  },
};

module.exports = nextConfig;