import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export const useAuthStore = defineStore('auth', () => {
    const token = ref<string | null>(localStorage.getItem('chess_token'));
    const nickname = ref<string | null>(localStorage.getItem('chess_nickname'));
    const guestId = ref<string | null>(localStorage.getItem('chess_guest_id'));

    const isAuthenticated = computed(() => !!token.value);

    async function loginAsGuest() {
        try {
            const response = await fetch('http://localhost:8080/api/auth/guest', {
                method: 'POST',
            });
            if (!response.ok) throw new Error('Failed to login as guest');
            
            const data = await response.json();
            
            token.value = data.token;
            nickname.value = data.nickname;
            guestId.value = data.guestId;

            localStorage.setItem('chess_token', data.token);
            localStorage.setItem('chess_nickname', data.nickname);
            localStorage.setItem('chess_guest_id', data.guestId);
        } catch (error) {
            console.error('Guest login error:', error);
            throw error;
        }
    }

    function logout() {
        token.value = null;
        nickname.value = null;
        guestId.value = null;
        localStorage.removeItem('chess_token');
        localStorage.removeItem('chess_nickname');
        localStorage.removeItem('chess_guest_id');
    }

    return {
        token,
        nickname,
        guestId,
        isAuthenticated,
        loginAsGuest,
        logout,
    };
});
