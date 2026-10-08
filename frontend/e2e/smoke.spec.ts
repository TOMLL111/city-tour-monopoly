import { expect, test } from '@playwright/test'

test('home page can create a room', async ({ page }) => {
  const nickname = `测试${Date.now().toString().slice(-6)}`
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '把好友拉进同一座城市' })).toBeVisible()
  await page.getByPlaceholder('最多 16 个字符').fill(nickname)
  await page.getByRole('button', { name: '创建房间' }).click()
  await expect(page.getByText('好友房间')).toBeVisible()
  await expect(page.getByRole('heading', { name: /^[A-Z2-9]{6}$/ })).toBeVisible()
  await expect(page.locator('.player-seat strong').filter({ hasText: nickname }).first()).toBeVisible()
})