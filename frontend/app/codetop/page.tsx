import OliverPage from "@/components/codetop/codetop-page"
import Layout from "@/components/kokonutui/layout"
import ProtectedRoute from "@/components/auth/protected-route"

export default function Oliver() {
  return (
    <ProtectedRoute>
      <Layout>
        <OliverPage />
      </Layout>
    </ProtectedRoute>
  )
}
