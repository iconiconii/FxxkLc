import AuthLayout from "@/components/auth/auth-layout"
import RegisterForm from "@/components/auth/register-form"
import ProtectedRoute from "@/components/auth/protected-route"

export const metadata = {
  title: "注册 - CodeTop",
  description: "创建您的 CodeTop 账户",
}

export default function RegisterPage() {
  return (
    <ProtectedRoute requiredAuth={false}>
      <AuthLayout>
        <RegisterForm />
      </AuthLayout>
    </ProtectedRoute>
  )
}
