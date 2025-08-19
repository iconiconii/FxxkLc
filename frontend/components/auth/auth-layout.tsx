"use client"

import type React from "react"

import Image from "next/image"
import Link from "next/link"
import { ThemeToggle } from "../theme-toggle"

interface AuthLayoutProps {
  children: React.ReactNode
}

export default function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-[#0F0F12] flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      {/* Header */}
      <div className="absolute top-4 left-4 right-4 flex items-center justify-between">
        <Link href="/" className="flex items-center gap-3">
          <Image
            src="https://kokonutui.com/logo.svg"
            alt="OLIVER"
            width={32}
            height={32}
            className="flex-shrink-0 hidden dark:block"
          />
          <Image
            src="https://kokonutui.com/logo-black.svg"
            alt="OLIVER"
            width={32}
            height={32}
            className="flex-shrink-0 block dark:hidden"
          />
          <span className="text-xl font-bold text-gray-900 dark:text-white">OLIVER</span>
          <span className="text-xl">🔥</span>
        </Link>
        <ThemeToggle />
      </div>

      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="text-center mb-8">
          <div className="flex justify-center items-center gap-3 mb-4">
            <Image
              src="https://kokonutui.com/logo.svg"
              alt="OLIVER"
              width={48}
              height={48}
              className="hidden dark:block"
            />
            <Image
              src="https://kokonutui.com/logo-black.svg"
              alt="OLIVER"
              width={48}
              height={48}
              className="block dark:hidden"
            />
          </div>
        </div>

        <div className="bg-white dark:bg-[#1F1F23] py-8 px-4 shadow-lg sm:rounded-lg sm:px-10 border border-gray-200 dark:border-[#2B2B30]">
          {children}
        </div>
      </div>

      {/* Footer */}
      <div className="mt-8 text-center">
        <p className="text-sm text-gray-500 dark:text-gray-400">© 2025 OLIVER. 专注于算法面试题目练习</p>
      </div>
    </div>
  )
}
