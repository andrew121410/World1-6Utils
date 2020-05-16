package com.andrew121410.mc.world16utils.sign.screen;

import com.andrew121410.mc.world16utils.chat.LanguageLocale;
import com.andrew121410.mc.world16utils.sign.SignCache;
import com.andrew121410.mc.world16utils.sign.SignUtils;
import com.google.common.collect.Lists;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SignScreenManager {

    private JavaPlugin plugin;

    private String name;
    private Location location;

    private ISignScreen signScreen;

    private int line = 0;
    private int scroll = 0;
    private int min = 0;
    private int max = 3;
    private final int maxText = 15;

    //Patterns
    private int side = 0;
    private int patternBuffer = 0;
    private int patternBufferNumber = 0;
    private List<String> pattern;

    private SignCache signCache1;

    private boolean hasTextChanged = false;
    private boolean hasScrollChanged = false;
    private boolean hasSidewaysChanged = false;
    private boolean hasLineChanged = false;

    private boolean isTickerRunning = false;
    private boolean stop = false;

    public static final long DEFAULT_TICK_SPEED = 10L;
    private long tickSpeed;

    private List<List<String>> partition = null;

    public SignScreenManager(JavaPlugin plugin, Location location, String name, Long tickSpeed, ISignScreen signScreen) {
        this.plugin = plugin;
        this.name = name;
        this.location = location;

        setPattern(new String[]{"A", "A", "A", "A"});

        this.signScreen = signScreen;

        if (tickSpeed == null) {
            this.tickSpeed = DEFAULT_TICK_SPEED;
        } else this.tickSpeed = tickSpeed;

        Sign sign = SignUtils.isSign(location.getBlock());
        if (sign != null) {
            tick();
            this.signScreen.onButton(this, null, 0, 0);
        }
    }

    public void onClick(Player player) {
        if (!this.isTickerRunning) {
            tick(player);
            return;
        }

        Sign sign = SignUtils.isSign(location.getBlock());
        if (sign != null) this.signScreen.onButton(this, player, line, scroll);
    }

    public void onScroll(Player player, boolean up) {
        if (!this.isTickerRunning) {
            tick(player);
            return;
        }

        if (this.partition == null) {
            return;
        }

        Sign sign = SignUtils.isSign(location.getBlock());
        if (sign != null) {
            if (up) {
                int upONE = this.scroll + 1;
                if (upONE < this.partition.size()) {
                    this.scroll++;
                    for (int i = 0; i <= 3; i++) {
                        String line;
                        if (i < this.partition.get(this.scroll).size()) {
                            line = this.partition.get(this.scroll).get(i);
                        } else line = "";
                        sign.setLine(i, line);
                    }
                    this.signCache1 = new SignCache(sign);
                    this.hasScrollChanged = true;
                }
            } else {
                int downONE = this.scroll - 1;
                if (downONE >= 0 && downONE <= this.partition.size()) {
                    this.scroll--;
                    for (int i = 0; i <= 3; i++) {
                        String line;
                        if (i < this.partition.get(this.scroll).size()) {
                            line = this.partition.get(this.scroll).get(i);
                        } else line = "";
                        sign.setLine(i, line);
                    }
                    this.signCache1 = new SignCache(sign);
                    this.hasScrollChanged = true;
                }
            }
        }
    }

    public void changeLines(Player player) {
        if (!this.isTickerRunning) {
            tick(player);
            return;
        }

        int toSide = side + 1;
        String pattern = this.pattern.get(side);
        long howManyAs = pattern.chars().filter(it -> it == 'A').count();

        if (toSide == howManyAs) {
            if (this.min <= this.line && this.line < this.max) {
                line++;
            } else {
                line = min;
            }
            hasLineChanged = true;
            return;
        }

        AtomicInteger atomicInteger = new AtomicInteger(0);
        pattern.chars().forEach(c -> {
            String charString = String.valueOf(c);

            if (charString.equalsIgnoreCase("A")) {
                if (toSide == atomicInteger.get()) {
                    patternBuffer = atomicInteger.get();
                }
                atomicInteger.getAndIncrement();
            }
        });
        hasSidewaysChanged = true;
    }

    public void tick(Player player) {
        if (!this.isTickerRunning) {
            tick();
            player.sendMessage(LanguageLocale.color("&2Sign turned on."));
        }
    }

    private void tick() {
        if (!this.isTickerRunning) {
            this.isTickerRunning = true;
            Sign sign = SignUtils.isSign(location.getBlock());

            if (sign == null) throw new NullPointerException("SignScreenManager : tick() : sign == null : NULL");

            new BukkitRunnable() {
                int pointer = 0;
                private boolean hold = false;
                private final SignCache signCacheBuffer = new SignCache(sign);
                int oldLine = line;
                private StringBuffer stringBuffer = new StringBuffer();

                @Override
                public void run() {
                    if (stop && pointer == 0) {
                        isTickerRunning = false;
                        stop = false;
                        this.cancel();
                        return;
                    }

                    //Holding
                    if (hold) return;

                    if (pointer == 0 && !hasTextChanged && !hasScrollChanged && !hasSidewaysChanged && !hasLineChanged) {
                        //Save line number and sign lines before we make changes.
                        line = this.oldLine;
                        this.signCacheBuffer.fromSign(sign);

                        //Clear string buffer.
                        this.stringBuffer = new StringBuffer();

                        //Gets the text from the sign.
                        String text = sign.getLine(line);
                        this.stringBuffer.append(text);

                        this.stringBuffer.setCharAt(patternBuffer, '>');
                        sign.setLine(line, this.stringBuffer.toString());
                        if (!sign.update()) stop = true;
                        pointer++;
                    } else if (pointer > 0 && !hasTextChanged && !hasScrollChanged && !hasSidewaysChanged && !hasLineChanged) {
                        this.stringBuffer.setCharAt(patternBuffer, ' ');
                        sign.setLine(line, this.stringBuffer.toString());
                        if (!sign.update()) stop = true;
                        pointer = 0;
                    } else if (hasLineChanged) {
                        side = 0;
                        pointer = 0;
                        hasLineChanged = false;
                    } else if (hasTextChanged) {
                        signCache1.update(sign);
                        partition = null;
                        oldLine = line;
                        pointer = 0;
                        hasTextChanged = false;
                    } else if (hasScrollChanged) {
                        signCache1.update(sign);
                        oldLine = line;
                        pointer = 0;
                        hasScrollChanged = false;
                    } else if (hasSidewaysChanged) {
                        pointer = 0;
                        hasSidewaysChanged = false;
                    }
                }
            }.runTaskTimer(this.plugin, this.tickSpeed, this.tickSpeed);
        }
    }

    public void updateSign(Sign sign) {
        tick();
        this.signCache1 = new SignCache(sign);
        this.hasTextChanged = true;
    }

    public void updateSign(Sign sign, List<String> stringList) {
        tick();
        this.partition = Lists.partition(stringList, 4);
        for (int i = 0; i <= 3; i++) {
            String line;
            if (i < this.partition.get(0).size()) {
                line = this.partition.get(0).get(i);
            } else line = "";
            sign.setLine(i, line);
        }
        this.signCache1 = new SignCache(sign);
        this.hasScrollChanged = true;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
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

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getScroll() {
        return scroll;
    }

    public void setScroll(int scroll) {
        this.scroll = scroll;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public SignCache getSignCache1() {
        return signCache1;
    }

    public void setSignCache1(SignCache signCache1) {
        this.signCache1 = signCache1;
    }

    public boolean isHasTextChanged() {
        return hasTextChanged;
    }

    public void setHasTextChanged(boolean hasTextChanged) {
        this.hasTextChanged = hasTextChanged;
    }

    public boolean isHasScrollChanged() {
        return hasScrollChanged;
    }

    public void setHasScrollChanged(boolean hasScrollChanged) {
        this.hasScrollChanged = hasScrollChanged;
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

    public List<List<String>> getPartition() {
        return partition;
    }

    public void setPartition(List<List<String>> partition) {
        this.partition = partition;
    }

    public Sign getSign() {
        return SignUtils.isSign(location.getBlock());
    }

    public long getTickSpeed() {
        return tickSpeed;
    }

    public void setTickSpeed(long tickSpeed) {
        this.tickSpeed = tickSpeed;
    }

    public int getSide() {
        return side;
    }

    public void setSide(int side) {
        this.side = side;
    }

    public int getMaxText() {
        return maxText;
    }

    public boolean isHasSidewaysChanged() {
        return hasSidewaysChanged;
    }

    public void setHasSidewaysChanged(boolean hasSidewaysChanged) {
        this.hasSidewaysChanged = hasSidewaysChanged;
    }

    public List<String> getPattern() {
        return pattern;
    }

    public void setPattern(String[] pattern) {
        for (int i = 0; i < pattern.length; i++) {
            String text = pattern[i];
            text = text.replaceAll("#", " ");
            pattern[i] = text;
        }

        this.pattern = Arrays.asList(pattern);
    }
}