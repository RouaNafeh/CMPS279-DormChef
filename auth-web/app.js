 import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
  import {
    getAuth,
    verifyPasswordResetCode,
    confirmPasswordReset,
    applyActionCode
  } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

 const firebaseConfig = {
  apiKey: "AIzaSyAW0WeDWOp4-PLx5g8v0nCjQMdd3ugDZ7A",
  authDomain: "cooksy-ef10e.firebaseapp.com",
  projectId: "cooksy-ef10e",
  storageBucket: "cooksy-ef10e.firebasestorage.app",
  messagingSenderId: "125361919350",
  appId: "1:125361919350:web:df1279c4a432bd722a9657",
  measurementId: "G-MQX45WH7HQ"
};

  const app = initializeApp(firebaseConfig);
  const auth = getAuth(app);

  const title = document.getElementById("title");
  const message = document.getElementById("message");
  const resetForm = document.getElementById("resetForm");
  const newPassword = document.getElementById("newPassword");
  const backButton = document.getElementById("backButton");

  const params = new URLSearchParams(window.location.search);
  const mode = params.get("mode");
  const oobCode = params.get("oobCode");

  function buildCookioDeepLink() {
    const targetMode = mode || "auth";
    return `cookio://auth-complete?mode=${encodeURIComponent(targetMode)}`;
  }

  function showDone() {
    backButton.classList.remove("hidden");
    backButton.onclick = () => {
      window.location.href = buildCookioDeepLink();
    };
  }

  async function handleResetPassword() {
    title.textContent = "Reset password";
    message.textContent = "Enter a new password for your Cookio account.";

    try {
      await verifyPasswordResetCode(auth, oobCode);
      resetForm.classList.remove("hidden");
    } catch {
      title.textContent = "Link expired";
      message.textContent = "This password reset link is invalid or expired.";
      showDone();
      return;
    }

    resetForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      try {
        await confirmPasswordReset(auth, oobCode, newPassword.value);
        resetForm.classList.add("hidden");
        title.textContent = "Password updated";
        message.textContent = "Your password was changed successfully. You can return to Cookio and log in.";
        showDone();
      } catch (err) {
        message.textContent = err.message || "Could not reset password.";
      }
    });
  }

  async function handleVerifyEmail() {
    title.textContent = "Verify email";
    message.textContent = "Confirming your Cookio email address.";

    try {
      await applyActionCode(auth, oobCode);
      title.textContent = "Email verified";
      message.textContent = "Your email is now verified. You can return to Cookio and log in.";
    } catch {
      title.textContent = "Verification failed";
      message.textContent = "This verification link is invalid or expired.";
    }

    showDone();
  }

  if (!mode || !oobCode) {
    title.textContent = "Invalid link";
    message.textContent = "This action link is missing required information.";
    showDone();
  } else if (mode === "resetPassword") {
    handleResetPassword();
  } else if (mode === "verifyEmail") {
    handleVerifyEmail();
  } else {
    title.textContent = "Unsupported action";
    message.textContent = "This link type is not supported on this page.";
    showDone();
  }
