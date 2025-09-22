import { render, screen, fireEvent } from '@testing-library/react'
import { Button } from '../Button'

describe('Button Component', () => {
  it('renders with default props', () => {
    render(<Button>Click me</Button>)
    const button = screen.getByRole('button', { name: /click me/i })
    expect(button).toBeInTheDocument()
    expect(button).toHaveClass('inline-flex', 'items-center', 'justify-center')
  })

  it('renders with different variants', () => {
    const { rerender } = render(<Button variant="solid">Solid</Button>)
    expect(screen.getByRole('button')).toHaveClass('bg-primary', 'text-primary-foreground')

    rerender(<Button variant="outline">Outline</Button>)
    expect(screen.getByRole('button')).toHaveClass('border-2', 'border-primary', 'bg-transparent')

    rerender(<Button variant="ghost">Ghost</Button>)
    expect(screen.getByRole('button')).toHaveClass('bg-transparent', 'text-primary')
  })

  it('renders with different sizes', () => {
    const { rerender } = render(<Button size="sm">Small</Button>)
    expect(screen.getByRole('button')).toHaveClass('h-8', 'px-3', 'text-xs')

    rerender(<Button size="md">Medium</Button>)
    expect(screen.getByRole('button')).toHaveClass('h-10', 'px-4', 'text-sm')

    rerender(<Button size="lg">Large</Button>)
    expect(screen.getByRole('button')).toHaveClass('h-11', 'px-6', 'text-base')
  })

  it('renders with different shapes', () => {
    const { rerender } = render(<Button shape="rounded">Rounded</Button>)
    expect(screen.getByRole('button')).toHaveClass('rounded-md')

    rerender(<Button shape="pill">Pill</Button>)
    expect(screen.getByRole('button')).toHaveClass('rounded-full')

    rerender(<Button shape="square">Square</Button>)
    expect(screen.getByRole('button')).toHaveClass('rounded-none')
  })

  it('handles click events', () => {
    const handleClick = jest.fn()
    render(<Button onClick={handleClick}>Click me</Button>)
    
    fireEvent.click(screen.getByRole('button'))
    expect(handleClick).toHaveBeenCalledTimes(1)
  })

  it('shows loading state', () => {
    render(<Button loading>Loading</Button>)
    const button = screen.getByRole('button')
    
    expect(button).toBeDisabled()
    expect(button).toHaveClass('cursor-wait')
    expect(screen.getByText('Loading')).toHaveClass('opacity-0')
  })

  it('renders as disabled when disabled prop is true', () => {
    render(<Button disabled>Disabled</Button>)
    const button = screen.getByRole('button')
    
    expect(button).toBeDisabled()
    expect(button).toHaveClass('disabled:pointer-events-none', 'disabled:opacity-50')
  })

  it('renders with preset configurations', () => {
    const { rerender } = render(<Button preset="primary">Primary</Button>)
    expect(screen.getByRole('button')).toHaveClass('bg-primary', 'text-primary-foreground')

    rerender(<Button preset="secondary">Secondary</Button>)
    expect(screen.getByRole('button')).toHaveClass('border-2', 'border-primary', 'bg-transparent')

    rerender(<Button preset="danger">Danger</Button>)
    expect(screen.getByRole('button')).toHaveClass('bg-destructive', 'text-destructive-foreground')
  })

  it('renders as child component when asChild is true', () => {
    render(
      <Button asChild>
        <a href="/test">Link Button</a>
      </Button>
    )
    
    const link = screen.getByRole('link')
    expect(link).toBeInTheDocument()
    expect(link).toHaveAttribute('href', '/test')
  })

  it('applies custom className', () => {
    render(<Button className="custom-class">Custom</Button>)
    expect(screen.getByRole('button')).toHaveClass('custom-class')
  })

  it('forwards ref correctly', () => {
    const ref = jest.fn()
    render(<Button ref={ref}>Ref Button</Button>)
    expect(ref).toHaveBeenCalled()
  })
})

