import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export const useAuthStore = defineStore('auth', () => {
    const token = ref<string | null>(localStorage.getItem('chess_token'));
    const nickname = ref<string | null>(localStorage.getItem('chess_nickname'));
    const guestId = ref<string | null>(localStorage.getItem('chess_guest_id'));

    const isAuthenticated = computed(() => !!token.value);

    function isTokenExpired(): boolean {
        if (!token.value) return true;
        try {
            const payload = JSON.parse(atob(token.value.split('.')[1]));
            return payload.exp * 1000 < Date.now();
        } catch {
            return true;
        }
    }

    async function loginAsGuest() {
        try {
            const response = await fetch('/api/auth/guest', {
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
        isTokenExpired,
        loginAsGuest,
        logout,
    };
});
