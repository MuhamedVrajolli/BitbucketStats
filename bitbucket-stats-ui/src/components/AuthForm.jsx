import { useState } from 'react'
import { ChevronDown, ChevronUp, Key, User, Building } from 'lucide-react'

export default function AuthForm({ credentials, setCredentials }) {
  const [isExpanded, setIsExpanded] = useState(!credentials)
  const [formData, setFormData] = useState({
    username: credentials?.username || '',
    appPassword: credentials?.appPassword || '',
    workspace: credentials?.workspace || '',
  })

  const handleSubmit = (e) => {
    e.preventDefault()
    if (formData.username && formData.appPassword && formData.workspace) {
      setCredentials(formData)
      setIsExpanded(false)
    }
  }

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
  }

  const handleClear = () => {
    setCredentials(null)
    setFormData({ username: '', appPassword: '', workspace: '' })
    setIsExpanded(true)
  }

  return (
    <div className="bg-white rounded-xl border border-gray-200 card-shadow">
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className="w-full flex items-center justify-between p-4 text-left hover:bg-gray-50 rounded-xl transition-colors"
      >
        <div className="flex items-center gap-3">
          <Key className="h-5 w-5 text-gray-500" />
          <span className="font-medium text-gray-900">
            {credentials ? `Connected as ${credentials.username}` : 'Bitbucket Credentials'}
          </span>
          {credentials && (
            <span className="px-2 py-0.5 text-xs font-medium bg-green-100 text-green-700 rounded-full">
              Connected
            </span>
          )}
        </div>
        {isExpanded ? (
          <ChevronUp className="h-5 w-5 text-gray-400" />
        ) : (
          <ChevronDown className="h-5 w-5 text-gray-400" />
        )}
      </button>

      {isExpanded && (
        <form onSubmit={handleSubmit} className="p-4 pt-0 space-y-4">
          <div className="grid gap-4 sm:grid-cols-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                <User className="inline h-4 w-4 mr-1" />
                Username
              </label>
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                placeholder="Your Bitbucket username"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                <Key className="inline h-4 w-4 mr-1" />
                App Password
              </label>
              <input
                type="password"
                name="appPassword"
                value={formData.appPassword}
                onChange={handleChange}
                placeholder="Your app password"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                <Building className="inline h-4 w-4 mr-1" />
                Workspace
              </label>
              <input
                type="text"
                name="workspace"
                value={formData.workspace}
                onChange={handleChange}
                placeholder="e.g., acme-corp"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
                required
              />
            </div>
          </div>

          <div className="flex gap-2">
            <button
              type="submit"
              className="px-4 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors text-sm"
            >
              Connect
            </button>
            {credentials && (
              <button
                type="button"
                onClick={handleClear}
                className="px-4 py-2 bg-gray-100 text-gray-700 font-medium rounded-lg hover:bg-gray-200 transition-colors text-sm"
              >
                Disconnect
              </button>
            )}
          </div>
        </form>
      )}
    </div>
  )
}
