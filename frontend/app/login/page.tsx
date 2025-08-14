import AuthLayout from "@/components/auth/auth-layout"
import LoginForm from "@/components/auth/login-form"

export const metadata = {
  title: "登录 - CodeTop",
  description: "登录您的 CodeTop 账户",
}

export default function LoginPage() {
  return (
    <AuthLayout>
      <LoginForm />
    </AuthLayout>
  )
}
