import ReportForm from "@/components/report/report-form"
import Layout from "@/components/kokonutui/layout"
import ProtectedRoute from "@/components/auth/protected-route"

export default function ReportPage() {
  return (
    <Layout>
      <ProtectedRoute>
        <ReportForm />
      </ProtectedRoute>
    </Layout>
  )
}