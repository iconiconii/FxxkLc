import AuthLayout from "@/components/auth/auth-layout"
import LoginForm from "@/components/auth/login-form"
import ProtectedRoute from "@/components/auth/protected-route"

export const metadata = {
  title: "登录 - CodeTop",
  description: "登录您的 CodeTop 账户",
}

export default function LoginPage() {
  return (
    <ProtectedRoute requiredAuth={false}>
      <AuthLayout>
        <LoginForm />
      </AuthLayout>
    </ProtectedRoute>
  )
}
