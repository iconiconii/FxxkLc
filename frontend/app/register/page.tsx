import AuthLayout from "@/components/auth/auth-layout"
import { Button } from "@/components/ui/button"
import Link from "next/link"

export const metadata = {
  title: "注册暂时关闭 - OLIVER",
  description: "注册功能暂时关闭"
}

export default function RegisterPage() {
  return (
    <AuthLayout>
      <div className="space-y-6 text-center">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white">注册功能暂时关闭</h2>
          <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">系统维护中，注册功能暂时不可用</p>
        </div>
        
        <div className="bg-yellow-50 dark:bg-yellow-900/50 border border-yellow-200 dark:border-yellow-800 text-yellow-700 dark:text-yellow-400 px-4 py-3 rounded">
          为了确保系统稳定性，我们暂时关闭了用户注册功能。感谢您的理解。
        </div>
        
        <div className="space-y-4">
          <p className="text-sm text-gray-600 dark:text-gray-400">
            如果您已有账户，可以继续登录使用
          </p>
          <Button asChild className="w-full h-11 bg-blue-600 hover:bg-blue-700">
            <Link href="/login">返回登录</Link>
          </Button>
        </div>
      </div>
    </AuthLayout>
  )
}
