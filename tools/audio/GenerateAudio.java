import java.io.*;
import java.util.*;

/**
 * 《仕途》占位音频生成器（纯 JDK，无第三方依赖）。
 *
 * 生成 8 首循环 BGM 与 12 个界面/事件音效，风格为
 * 「现代影视配乐 + 轻东方氛围」：温和钢琴、柔和弦乐铺底、极轻低频 Bass，
 * 66~76 BPM，无鼓点、无刺耳高频。所有频率都量化到循环周期的整数分之一，
 * 因此循环点天然无缝；占位音频可直接替换为同名 ogg/mp3 而不改代码。
 */
public class GenerateAudio {

    static final int SR = 22050;

    // 和弦：{低音, 铺垫音...}
    static final int[][] C_MAJ7 = {{48, 60, 64, 67, 71}};
    static final int[][] A_M7   = {{45, 57, 60, 64, 67}};
    static final int[][] F_MAJ7 = {{41, 53, 57, 60, 64}};
    static final int[][] G_6    = {{43, 55, 59, 62, 64}};
    static final int[][] D_M7   = {{38, 50, 53, 57, 60}};
    static final int[][] E_M7   = {{40, 52, 55, 59, 62}};
    static final int[][] E_7    = {{40, 52, 56, 59, 62}};

    static final int[] C_PENT = {60, 62, 64, 67, 69, 72, 74, 76, 79, 81};
    static final int[] A_PENT = {57, 60, 62, 64, 67, 69, 72, 74, 76, 79};

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "app/src/main/assets/audio";
        new File(out, "bgm").mkdirs();
        new File(out, "ui").mkdirs();
        new File(out, "sfx").mkdirs();

        // ===== BGM =====
        bgm(out, "menu", 72, new int[][][]{C_MAJ7, A_M7, F_MAJ7, G_6}, C_PENT,
                0.42, 0.90, 1.00, 0.70, 101, false);
        bgm(out, "home", 72, new int[][][]{F_MAJ7, C_MAJ7, D_M7, G_6}, C_PENT,
                0.50, 0.95, 1.00, 0.72, 202, false);
        bgm(out, "governance", 76, new int[][][]{C_MAJ7, F_MAJ7, A_M7, G_6}, C_PENT,
                0.34, 1.00, 0.85, 0.68, 303, false);
        bgm(out, "event", 70, new int[][][]{A_M7, F_MAJ7, C_MAJ7, G_6}, A_PENT,
                0.38, 1.00, 0.95, 0.70, 404, false);
        // 重大事件：更厚一点的弦乐，末句用属七和弦带来克制的紧张感。
        bgm(out, "major", 68, new int[][][]{A_M7, F_MAJ7, D_M7, E_7}, A_PENT,
                0.30, 1.10, 0.78, 0.86, 909, false);
        bgm(out, "politics", 68, new int[][][]{D_M7, A_M7, F_MAJ7, G_6}, A_PENT,
                0.28, 1.10, 0.80, 0.80, 505, false);
        bgm(out, "crisis", 66, new int[][][]{A_M7, E_M7, F_MAJ7, D_M7}, A_PENT,
                0.20, 1.20, 0.62, 0.90, 606, false);
        bgm(out, "ending", 68, new int[][][]{C_MAJ7, A_M7, F_MAJ7, G_6}, C_PENT,
                0.40, 1.00, 0.95, 0.70, 707, false);
        bgm(out, "promotion", 74, new int[][][]{F_MAJ7, C_MAJ7, G_6, A_M7, F_MAJ7, C_MAJ7}, C_PENT,
                0.56, 0.95, 1.00, 0.75, 808, true);

        // ===== UI 音效（轻、短、干净）=====
        sfx(out, "ui/click", 0.09, new double[][]{
                {0.000, 0.070, 1180, 0.32, 0.022, 3},
                {0.000, 0.030, 590, 0.12, 0.020, 2}});
        sfx(out, "ui/page", 0.18, new double[][]{
                {0.000, 0.090, 784, 0.26, 0.030, 2},
                {0.060, 0.100, 1046, 0.22, 0.030, 2}});
        sfx(out, "ui/back", 0.18, new double[][]{
                {0.000, 0.090, 880, 0.24, 0.030, 2},
                {0.060, 0.100, 660, 0.20, 0.030, 2}});
        sfx(out, "ui/close", 0.22, new double[][]{
                {0.000, 0.130, 740, 0.22, 0.040, 2},
                {0.070, 0.120, 494, 0.18, 0.040, 2}});
        sfx(out, "ui/confirm", 0.32, new double[][]{
                {0.000, 0.110, 523, 0.24, 0.030, 2},
                {0.090, 0.200, 784, 0.22, 0.050, 2}});
        sfx(out, "ui/toggle", 0.09, new double[][]{
                {0.000, 0.060, 1320, 0.22, 0.018, 2}});

        // ===== 事件 / 里程碑音效 =====
        sfx(out, "sfx/warning", 0.42, new double[][]{
                {0.000, 0.320, 220, 0.26, 0.130, 2},
                {0.000, 0.320, 330, 0.12, 0.130, 2}});
        sfx(out, "sfx/success", 0.48, new double[][]{
                {0.000, 0.140, 587, 0.22, 0.040, 2},
                {0.100, 0.320, 880, 0.20, 0.090, 2}});
        sfx(out, "sfx/failure", 0.48, new double[][]{
                {0.000, 0.140, 440, 0.20, 0.050, 2},
                {0.110, 0.320, 330, 0.18, 0.100, 2}});
        sfx(out, "sfx/event_major", 0.75, new double[][]{
                {0.000, 0.520, 196, 0.24, 0.260, 3},
                {0.020, 0.520, 294, 0.14, 0.260, 2},
                {0.120, 0.520, 392, 0.10, 0.220, 2}});
        sfx(out, "sfx/achievement", 0.95, new double[][]{
                {0.000, 0.240, 659, 0.20, 0.070, 3},
                {0.120, 0.260, 830, 0.18, 0.070, 3},
                {0.240, 0.480, 988, 0.20, 0.140, 3}});
        sfx(out, "sfx/promotion", 1.70, new double[][]{
                {0.000, 0.520, 523, 0.20, 0.300, 3},
                {0.180, 0.520, 659, 0.20, 0.300, 3},
                {0.360, 0.620, 784, 0.20, 0.400, 3},
                {0.560, 0.920, 1046, 0.22, 0.500, 3},
                {0.560, 0.920, 1318, 0.08, 0.500, 2}});
        sfx(out, "sfx/ending", 2.10, new double[][]{
                {0.000, 1.200, 392, 0.18, 0.620, 3},
                {0.100, 1.200, 523, 0.16, 0.620, 3},
                {0.500, 1.400, 659, 0.14, 0.720, 3},
                {0.900, 1.000, 784, 0.10, 0.600, 2}});

        System.out.println("音频生成完成：" + out);
    }

    // ===== BGM 合成 =====

    static void bgm(String out, String name, int bpm, int[][][] chords, int[] scale,
                    double density, double padGain, double pianoGain, double bassGain,
                    int seed, boolean build) throws Exception {
        double beat = 60.0 / bpm;
        double bar = beat * 4;
        int bars = chords.length * 2;
        double loopSec = bars * bar;
        int n = (int) Math.round(loopSec * SR);
        double[] buf = new double[n];
        Random rng = new Random(seed);

        // 铺垫弦乐：每个和弦一组，长起音、长释放。
        for (int c = 0; c < chords.length; c++) {
            double start = c * 2 * bar;
            double dur = 2 * bar + 1.0;
            int[] spec = chords[c][0];
            // 晋升曲：弦乐在后半段逐渐展开，从铺垫走向开阔。
            double swell = build ? (0.55 + 0.45 * c / (double) (chords.length - 1)) : 1.0;
            for (int k = 1; k < spec.length; k++) {
                double f = midi(spec[k]);
                double amp = padGain * 0.085 * (k == 1 ? 1.15 : 1.0) * swell;
                add(buf, start, dur, q(f, loopSec), amp, 1.6, 9.0, 2.0, new double[]{1.0, 0.30, 0.13, 0.05});
                add(buf, start, dur, q(f * 1.0015, loopSec), amp * 0.7, 1.8, 9.0, 2.0, new double[]{1.0, 0.30, 0.13, 0.05});
            }
            // 低音：每小节一次，柔和。
            for (int h = 0; h < 2; h++) {
                double bs = start + h * bar;
                add(buf, bs, bar * 1.2, q(midi(spec[0]), loopSec), bassGain * 0.13, 0.10, 4.0, 1.0,
                        new double[]{1.0, 0.16, 0.05});
            }
        }

        // 钢琴旋律：五声音阶上缓慢游走，强拍更密。
        int idx = scale.length / 2;
        int strong = 0;
        for (int b = 0; b < bars; b++) {
            double barStart = b * bar;
            double growth = build ? (0.35 + 0.65 * b / (double) (bars - 1)) : 1.0;
            for (int bt = 0; bt < 4; bt++) {
                double p = density;
                if (bt == 0) p += 0.28; else if (bt == 2) p += 0.12; else p *= 0.62;
                if (rng.nextDouble() > p) continue;
                if (strong == 0) idx += rng.nextInt(3) - 1;
                else idx += rng.nextInt(5) - 2;
                idx = Math.max(0, Math.min(scale.length - 1, idx));
                strong = (strong + 1) % 2;
                double f = midi(scale[idx]);
                double dur = beat * (1.5 + rng.nextDouble() * 1.4);
                double amp = pianoGain * growth * (0.115 + rng.nextDouble() * 0.035);
                add(buf, barStart + bt * beat, dur, q(f, loopSec), amp, 0.008, 0.40, 0.55,
                        new double[]{1.0, 0.30, 0.13, 0.05});
            }
            // 每四小节加一点高音星光，让长循环不单调。
            if (b % 4 == 2) {
                double f = midi(scale[scale.length - 1 - (b / 4) % 2]);
                add(buf, barStart + 2 * beat, 2.2, q(f, loopSec), 0.055, 0.02, 1.30, 0.8,
                        new double[]{1.0, 0.22, 0.08});
            }
        }

        // 归一化到较低电平，再在首尾做极短淡入淡出，保证循环无爆音。
        normalize(buf, 0.62);
        fade(buf, 0.025);
        writeWav(new File(out, "bgm/" + name + ".wav"), buf);
    }

    // ===== 音效合成 =====

    /** 每个音符：{起始秒, 时长, 频率, 音量, 衰减时间常数, 谐波数} */
    static void sfx(String out, String name, double total, double[][] notes) throws Exception {
        int n = (int) Math.round(total * SR);
        double[] buf = new double[n];
        for (double[] s : notes) {
            int harmonics = (int) s[5];
            double[] h = new double[harmonics];
            for (int i = 0; i < harmonics; i++) h[i] = 1.0 / (1.0 + i * 2.4);
            add(buf, s[0], s[1], s[2], s[3], 0.005, s[4], 0.050, h);
        }
        normalize(buf, 0.75);
        fade(buf, 0.008);
        writeWav(new File(out, name + ".wav"), buf);
    }

    // ===== 基础工具 =====

    static double midi(int note) { return 440.0 * Math.pow(2.0, (note - 69) / 12.0); }

    /** 把频率量化到循环周期的整数分之一，使波形在循环点严格连续。 */
    static double q(double f, double loopSec) { return Math.round(f * loopSec) / loopSec; }

    static void add(double[] buf, double start, double dur, double f, double amp,
                    double attack, double tau, double release, double[] harm) {
        int i0 = Math.max(0, (int) (start * SR));
        int i1 = Math.min(buf.length, (int) ((start + dur + release) * SR));
        for (int i = i0; i < i1; i++) {
            double t = (i - i0) / (double) SR;
            double env;
            if (t < attack) env = attack <= 0 ? 1 : t / attack;
            else env = Math.exp(-(t - attack) / Math.max(0.02, tau));
            double left = dur + release - t;
            if (left < release) env *= Math.max(0, left / release);
            if (env <= 0) continue;
            double s = 0;
            for (int h = 0; h < harm.length; h++) {
                s += harm[h] * Math.sin(2 * Math.PI * f * (h + 1) * (i - i0) / (double) SR);
            }
            buf[i] += amp * env * s;
        }
    }

    static void normalize(double[] buf, double peak) {
        double max = 0;
        for (double v : buf) max = Math.max(max, Math.abs(v));
        if (max <= 1e-9) return;
        double g = peak / max;
        for (int i = 0; i < buf.length; i++) buf[i] *= g;
    }

    static void fade(double[] buf, double seconds) {
        int k = Math.min(buf.length / 2, (int) (seconds * SR));
        for (int i = 0; i < k; i++) {
            double g = i / (double) k;
            buf[i] *= g;
            buf[buf.length - 1 - i] *= g;
        }
    }

    static void writeWav(File f, double[] buf) throws IOException {
        byte[] data = new byte[buf.length * 2];
        for (int i = 0; i < buf.length; i++) {
            int v = (int) Math.round(Math.max(-1, Math.min(1, buf[i])) * 32767);
            data[i * 2] = (byte) (v & 0xFF);
            data[i * 2 + 1] = (byte) ((v >> 8) & 0xFF);
        }
        try (DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) {
            os.writeBytes("RIFF");
            os.writeInt(Integer.reverseBytes(36 + data.length));
            os.writeBytes("WAVEfmt ");
            os.writeInt(Integer.reverseBytes(16));
            os.writeShort(Short.reverseBytes((short) 1));
            os.writeShort(Short.reverseBytes((short) 1));
            os.writeInt(Integer.reverseBytes(SR));
            os.writeInt(Integer.reverseBytes(SR * 2));
            os.writeShort(Short.reverseBytes((short) 2));
            os.writeShort(Short.reverseBytes((short) 16));
            os.writeBytes("data");
            os.writeInt(Integer.reverseBytes(data.length));
            os.write(data);
        }
    }
}