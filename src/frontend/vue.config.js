const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true
})

module.exports = {
  devServer: {
    port: 8080
  },
  publicPath: process.env.NODE_ENV === 'production'
      ? '/pocketstats/'
      : '/'
};