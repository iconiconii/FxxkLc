import CodeTopPage from "@/components/codetop/codetop-page"
import Layout from "@/components/kokonutui/layout"
import ProtectedRoute from "@/components/auth/protected-route"

export default function CodeTop() {
  return (
    <ProtectedRoute>
      <Layout>
        <CodeTopPage />
      </Layout>
    </ProtectedRoute>
  )
}
