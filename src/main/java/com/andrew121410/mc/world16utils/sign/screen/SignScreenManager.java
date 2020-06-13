package com.andrew121410.mc.world16utils.sign.screen;

import com.andrew121410.mc.world16utils.chat.LanguageLocale;
import com.andrew121410.mc.world16utils.sign.SignCache;
import com.andrew121410.mc.world16utils.sign.SignUtils;
import com.andrew121410.mc.world16utils.sign.screen.pages.SignLayout;
import com.andrew121410.mc.world16utils.sign.screen.pages.SignPage;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SignScreenManager {

    private JavaPlugin plugin;

    private String name;
    private Location location;

    private ISignScreen signScreen;

    private SignLayout currentLayout;
    private SignPage currentPage;

    private int pointerLine = 0;
    private SignCache signCache = null;

    private boolean needsLineChanged;
    private boolean needsTextChanged;

    private final static int SIGN_MAX_TEXT = 15;
    private final static int SIGN_MIN = 0;
    private final static int SIGN_MAX = 3;

    private boolean isTickerRunning = false;
    private boolean stop = false;

    public static final long DEFAULT_TICK_SPEED = 10L;
    private long tickSpeed;

    public SignScreenManager(JavaPlugin plugin, Location location, String name, Long tickSpeed, ISignScreen signScreen) {
        this.plugin = plugin;
        this.name = name;
        this.location = location;

        this.signScreen = signScreen;

        if (tickSpeed == null) {
            this.tickSpeed = DEFAULT_TICK_SPEED;
        } else this.tickSpeed = tickSpeed;

        this.signScreen.onDoneConstructed(this);
    }

    public void onClick(Player player) {
        if (!isTickerRunning) {
            tick(player);
        }

        this.signScreen.onButton(this, player, this.currentLayout, this.currentPage, this.pointerLine);
    }

    public void onScroll(Player player, boolean up) {
        if (!isTickerRunning) {
            tick(player);
        }

        if (up) {
            if (this.pointerLine != this.currentPage.getMin()) {
                this.pointerLine--;
                this.needsLineChanged = true;
            } else {
                SignPage newPage = this.currentLayout.getReversePage(this.currentPage.getPageNumber());
                if (newPage == null) {
                    player.sendMessage(LanguageLocale.color("&4No new page was found going [UP]"));
                    return;
                }
                this.currentPage = newPage;
                this.pointerLine = newPage.getMax();
                this.signCache = newPage.toSignCache();
                this.needsTextChanged = true;
            }
        } else {
            if (this.pointerLine != this.currentPage.getMax()) {
                this.pointerLine++;
                this.needsLineChanged = true;
            } else {
                SignPage newPage = this.currentLayout.getNextPage(this.currentPage.getPageNumber());
                if (newPage == null) {
                    player.sendMessage(LanguageLocale.color("&4No new page was found going [DOWN]"));
                    return;
                }
                this.currentPage = newPage;
                this.pointerLine = newPage.getStartLine();
                this.signCache = newPage.toSignCache();
                this.needsTextChanged = true;
            }
        }
    }

    public void tick(Player player) {
        if (!this.isTickerRunning) {
            player.sendMessage(LanguageLocale.color("&bStarting tick() please wait..."));
            tick();
            player.sendMessage(LanguageLocale.color("&6Running!!!"));
        }
    }

    private void tick() {
        if (isTickerRunning) {
            return;
        }
        this.isTickerRunning = true;

        Sign sign = SignUtils.isSign(location.getBlock());
        if (sign == null) throw new NullPointerException("SignScreenManager : tick() : sign == null : NULL");

        new BukkitRunnable() {
            private boolean hold = false;
            private int pointerAt = 5;
            private int oldPointerLine = pointerLine;
            private final SignCache signCacheSave = new SignCache();
            private StringBuffer stringBuffer;

            @Override
            public void run() {
                if (stop && pointerAt == 0) {
                    isTickerRunning = false;
                    stop = false;
                    this.cancel();
                    return;
                }
                //Holding
                if (hold || signCache == null) return;

                if (!needsTextChanged && !needsLineChanged && this.pointerAt != 5) {
                    if (pointerAt == 0) {
                        clearStringBufferAndUpdate();
                        this.stringBuffer.insert(0, ">");
                        sign.setLine(this.oldPointerLine, LanguageLocale.color(this.stringBuffer.toString()));
                        if (!sign.update()) stop = true;
                        pointerAt++;
                    } else {
                        if (!signCacheSave.updateFancy(sign)) stop = true;
                        pointerAt--;
                    }
                } else if (needsTextChanged || this.pointerAt == 5) {
                    this.oldPointerLine = pointerLine;
                    signCache.updateFancy(sign);
                    this.signCacheSave.fromSign(sign);
                    clearStringBufferAndUpdate();
                    this.pointerAt = 0;
                    needsTextChanged = false;
                } else if (needsLineChanged) {
                    this.oldPointerLine = pointerLine;
                    clearStringBufferAndUpdate();
                    this.pointerAt = 0;
                    needsLineChanged = false;
                }
            }

            private void clearStringBufferAndUpdate() {
                this.stringBuffer = new StringBuffer();
                this.stringBuffer.append(this.signCacheSave.getLine(this.oldPointerLine));
            }
        }.runTaskTimer(plugin, this.tickSpeed, this.tickSpeed);
    }

    public void updateLayoutAndPage(SignLayout signLayout, int beginningPage) {
        if (signLayout == null) {
            throw new NullPointerException("SignScreenManager : goToLayoutAndPage(String, Int) : signLayout == null : NULL");
        }
        SignPage signPage = signLayout.getSignPage(beginningPage);
        if (signPage == null) {
            throw new NullPointerException("SignScreenManager : goToLayoutAndPage(String, Int) : signPage == null : NULL");
        }
        this.currentLayout = signLayout;
        this.currentPage = signPage;
        this.pointerLine = signPage.getStartLine();
        this.signCache = signPage.toSignCache();
        this.needsTextChanged = true;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public ISignScreen getSignScreen() {
        return signScreen;
    }

    public void setSignScreen(ISignScreen signScreen) {
        this.signScreen = signScreen;
    }

    public SignLayout getCurrentLayout() {
        return currentLayout;
    }

    public void setCurrentLayout(SignLayout currentLayout) {
        this.currentLayout = currentLayout;
    }

    public SignPage getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(SignPage currentPage) {
        this.currentPage = currentPage;
    }

    public int getPointerLine() {
        return pointerLine;
    }

    public void setPointerLine(int pointerLine) {
        this.pointerLine = pointerLine;
    }

    public SignCache getSignCache() {
        return signCache;
    }

    public void setSignCache(SignCache signCache) {
        this.signCache = signCache;
    }

    public boolean isNeedsLineChanged() {
        return needsLineChanged;
    }

    public void setNeedsLineChanged(boolean needsLineChanged) {
        this.needsLineChanged = needsLineChanged;
    }

    public boolean isNeedsTextChanged() {
        return needsTextChanged;
    }

    public void setNeedsTextChanged(boolean needsTextChanged) {
        this.needsTextChanged = needsTextChanged;
    }

    public boolean isTickerRunning() {
        return isTickerRunning;
    }

    public void setTickerRunning(boolean tickerRunning) {
        isTickerRunning = tickerRunning;
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public long getTickSpeed() {
        return tickSpeed;
    }

    public void setTickSpeed(long tickSpeed) {
        this.tickSpeed = tickSpeed;
    }
}