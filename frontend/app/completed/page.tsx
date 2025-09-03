import CompletedProblems from "@/components/completed/completed-problems"
import ProtectedRoute from "@/components/auth/protected-route"

export default function CompletedPage() {
  return (
    <ProtectedRoute>
      <CompletedProblems />
    </ProtectedRoute>
  )
}