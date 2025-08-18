import LeaderboardPage from "@/components/leaderboard/leaderboard-page"
import Layout from "@/components/kokonutui/layout"
import ProtectedRoute from "@/components/auth/protected-route"

export default function Leaderboard() {
  return (
    <Layout>
      <ProtectedRoute>
        <LeaderboardPage />
      </ProtectedRoute>
    </Layout>
  )
}
