 <template>
   <div class="common-layout">
     <div class="login-bg">
       <div class="bg-overlay"></div>
     </div>
     <router-view />
   </div>
 </template>
 
 <script setup>
 </script>
 
 <style scoped>
 .common-layout {
   min-height: 100vh;
   display: flex;
   align-items: center;
   justify-content: center;
   position: relative;
   background: linear-gradient(135deg, #0b1a2e 0%, #1a3a5c 50%, #0d2137 100%);
 }
 .login-bg {
   position: fixed;
   top: 0;
   left: 0;
   width: 100%;
   height: 100%;
   background-image: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1440 800"><path fill="rgba(64,158,255,0.03)" d="M0,400 C360,200 720,600 1080,400 L1440,500 L1440,800 L0,800 Z"/><path fill="rgba(64,158,255,0.05)" d="M0,500 C240,300 600,700 960,500 L1440,600 L1440,800 L0,800 Z"/><circle cx="200" cy="150" r="3" fill="rgba(255,255,255,0.1)"/><circle cx="600" cy="100" r="2" fill="rgba(255,255,255,0.08)"/><circle cx="1000" cy="180" r="4" fill="rgba(255,255,255,0.06)"/><circle cx="1300" cy="120" r="2" fill="rgba(255,255,255,0.07)"/><circle cx="400" cy="300" r="3" fill="rgba(255,255,255,0.05)"/><circle cx="800" cy="250" r="2" fill="rgba(255,255,255,0.06)"/><circle cx="1100" cy="350" r="3" fill="rgba(255,255,255,0.04)"/></svg>');
   background-size: cover;
   background-position: center;
 }
 .bg-overlay {
   position: absolute;
   top: 0;
   left: 0;
   width: 100%;
   height: 100%;
   backdrop-filter: blur(0px);
 }
 </style>
