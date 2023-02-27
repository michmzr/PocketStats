<template>
  <div class="home">
    <TopAuth/>
  </div>
</template>

<script lang="ts">
import {Options, Vue} from 'vue-class-component';
import TopAuth from '@/components/TopAuth.vue';

@Options({
  components: {
    TopAuth
  },
})
export default class HomeView extends Vue {}
</script>
