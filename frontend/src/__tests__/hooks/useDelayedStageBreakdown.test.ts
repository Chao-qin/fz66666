import { createElement } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { render, renderHook, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import api from '@/utils/api';
import ProductionBottleneckCard from '@/modules/dashboard/components/ProductionBottleneckCard';
import { useDelayedStageBreakdown } from '@/modules/dashboard/components/DelayedStageBreakdown/useDelayedStageBreakdown';

afterEach(() => vi.restoreAllMocks());

describe('useDelayedStageBreakdown', () => {
  it.each([{}, { bulkDelayed: null, sampleDelayed: null }])(
    'shows no delayed stages when the API omits tenant data',
    async (data) => {
      vi.spyOn(api, 'get').mockResolvedValue({ code: 200, data });
      const { result, rerender } = renderHook(
        ({ tab }: { tab: 'bulk' | 'sample' }) => useDelayedStageBreakdown({ forceTab: tab }),
        { initialProps: { tab: 'bulk' } },
      );

      await waitFor(() => expect(result.current.data).not.toBeNull());
      expect(result.current.stageHints).toEqual([]);
      expect(result.current.total).toBe(0);

      rerender({ tab: 'sample' });
      expect(result.current.stageHints).toEqual([]);
      expect(result.current.total).toBe(0);
    },
  );

  it('renders the bottleneck card when no tenant data exists', async () => {
    vi.spyOn(api, 'get').mockResolvedValue({ code: 200, data: {} });
    render(createElement(MemoryRouter, null, createElement(ProductionBottleneckCard)));

    expect(await screen.findByText('大货各环节无延期')).toBeTruthy();
  });

  it('preserves stage filtering and links to the delayed orders', async () => {
    vi.spyOn(api, 'get').mockResolvedValue({
      code: 200,
      data: {
        bulkDelayed: [
          { stageName: '裁剪', count: 1, items: [{ id: 'order-1' }] },
          { stageName: '缝制', count: 1, items: [{ id: 'order-2' }] },
        ],
        bulkTotal: 2,
      },
    });
    const { result } = renderHook(() => useDelayedStageBreakdown({ stageFilter: '裁剪' }));

    await waitFor(() => expect(result.current.stageHints).toHaveLength(1));
    expect(result.current.total).toBe(1);
    expect(result.current.stageHints[0].buildNavigateUrl()).toBe('/production?orderIds=order-1');
  });
});
