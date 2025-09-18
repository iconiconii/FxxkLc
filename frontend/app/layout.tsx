'use client';

import { Inter } from "next/font/google"
import "./globals.css"
import { ThemeProvider } from "@/components/theme-provider"
import { AuthProvider } from "@/lib/auth-context"
import { RecommendationProvider } from "@/lib/recommendation-context"
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'

const inter = Inter({ subsets: ["latin"] })

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const [queryClient] = useState(() => new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60 * 1000, // 1 minute
        gcTime: 5 * 60 * 1000, // 5 minutes
      },
    },
  }));

  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <title>OLIVER FSRS - 智能算法学习平台</title>
        <meta name="description" content="基于 FSRS 算法的智能化算法题学习和复习系统" />
      </head>
      <body className={inter.className}>
        <QueryClientProvider client={queryClient}>
          <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
            <AuthProvider>
              <RecommendationProvider>
                {children}
              </RecommendationProvider>
            </AuthProvider>
          </ThemeProvider>
        </QueryClientProvider>
      </body>
    </html>
  )
}
