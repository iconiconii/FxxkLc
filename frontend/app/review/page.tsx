import ReviewPage from "@/components/review/review-page"
import Layout from "@/components/kokonutui/layout"
import ProtectedRoute from "@/components/auth/protected-route"

export default function Review() {
  return (
    <Layout>
      <ProtectedRoute>
        <ReviewPage />
      </ProtectedRoute>
    </Layout>
  )
}
