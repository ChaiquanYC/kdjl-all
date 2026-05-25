import { Component, type ReactNode } from 'react';

interface Props { children: ReactNode; }
interface State { hasError: boolean; error: Error | null; }

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, error: null };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          display: 'flex', flexDirection: 'column', alignItems: 'center',
          justifyContent: 'center', minHeight: '100vh', background: '#1a1a2e',
          color: '#eee', gap: 12,
        }}>
          <h2 style={{ color: '#e17055' }}>页面出错了</h2>
          <p style={{ color: '#a0a0b0', fontSize: 14, maxWidth: 400, textAlign: 'center' }}>
            {this.state.error?.message}
          </p>
          <button
            onClick={() => {
              localStorage.removeItem('token');
              this.setState({ hasError: false, error: null });
              window.location.href = '/login';
            }}
            style={{
              padding: '8px 24px', borderRadius: 6, border: 'none',
              background: '#e94560', color: '#fff', fontSize: 14, cursor: 'pointer',
            }}
          >
            返回登录页
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
