import AnalysisPage from "@/components/analysis/analysis-page"
import Layout from "@/components/kokonutui/layout"
import ProtectedRoute from "@/components/auth/protected-route"

export default function Analysis() {
  return (
    <Layout>
      <ProtectedRoute>
        <AnalysisPage />
      </ProtectedRoute>
    </Layout>
  )
}
