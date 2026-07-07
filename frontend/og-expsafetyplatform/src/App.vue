 <script setup>
 import zhCn from 'element-plus/es/locale/lang/zh-cn'
 </script>
 <template>
   <el-config-provider :locale="zhCn">
     <router-view />
   </el-config-provider>
 </template>
 <style>
 #app {
   width: 100%;
   height: 100vh;
 }
 * {
   margin: 0;
   padding: 0;
   box-sizing: border-box;
 }
 html, body {
   width: 100%;
   height: 100%;
   font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans SC', sans-serif;
 }
 </style>
