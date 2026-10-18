package s0594618;

import lenz.htw.ai4g.ai.AI;
import lenz.htw.ai4g.ai.Info;
import lenz.htw.ai4g.ai.PlayerAction;
import lenz.htw.ai4g.ai.DivingAction;
import lenz.htw.ai4g.ai.ShoppingAction;
import lenz.htw.ai4g.ai.ShoppingItem;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MeineKI extends AI {

    private ArrayList<Point> Perlenliste;
    private ArrayList<Point> originalPerlenliste; 
    private static final float Fischgefahrradius = 60f;

    private List<Point> langzeitPfad = new ArrayList<>();
    private Point letztesEndZiel = null;
    private static final int GRID_SIZE = 40;

    private ArrayList<Point> muellListe;
    private int altesGeld = 0;
    private boolean hatUpgrades = false;
    private int kaufSchritt = 0;

    private Point letztesZielPunkt = null;
    private boolean warenWirObenAmOberflächenlevel = false;

    public MeineKI(Info info) {
        super(info);
        enlistForTournament(594618);
        Perlenliste = new ArrayList<>(Arrays.asList(info.getScene().getPearl()));
        originalPerlenliste = new ArrayList<>(Perlenliste); // Backup speichern

        Point[] muell = info.getScene().getRecyclingProducts();
        if (muell != null) {
            muellListe = new ArrayList<>(Arrays.asList(muell));
        } else {
            muellListe = new ArrayList<>();
        }
    }

    @Override
    public String getName() {
        return "Amir";
    }

    @Override
    public Color getPrimaryColor() {
        return Color.BLUE;
    }

    @Override
    public Color getSecondaryColor() {
        return Color.RED;
    }

    private double Abstand(float perleX, float perleY, float meinX, float meinY) {
        return (perleX - meinX) * (perleX - meinX) + (perleY - meinY) * (perleY - meinY);
    }

    private Point NähestePerle(ArrayList<Point> Perlenliste, float myX, float myY) {
        if (Perlenliste.isEmpty()) return new Point((int)myX, (int)myY);

        Point näheste = Perlenliste.get(0);
        double kleinsterabstand = 999999999999.99;
        for (int i = 0; i < Perlenliste.size(); i++) {
            Point p = Perlenliste.get(i);
            double d = Abstand(p.x, p.y, myX, myY);
            if (d < kleinsterabstand) {
                kleinsterabstand = d;
                näheste = p;
            }
        }
        return näheste;
    }

    private float FischAusweichWinkel(float meinX, float meinY) {
        Point[] fische = info.getScene().getFish();
        if (fische == null || fische.length == 0) return Float.NaN;

        float ausweichX = 0, ausweichY = 0;
        boolean gefahr = false;

        for (Point f : fische) {
            float fx = (float) f.x;
            float fy = (float) f.y;

            double d = Math.sqrt(Abstand(fx, fy, meinX, meinY));
            if (d < Fischgefahrradius) {
                float stärke = (float)(Fischgefahrradius - d) / Fischgefahrradius;
                ausweichX += (meinX - fx) * stärke;
                ausweichY += (meinY - fy) * stärke;
                gefahr = true;
            }
        }

        if (!gefahr) return Float.NaN;
        return (float) Math.atan2(ausweichY, ausweichX);
    }

    private float StrömungsGegenlenkWinkel(float meinX, float meinY) {
        int margin = 5; 
        Rectangle[] linksStrömungen = info.getScene().getStreamsToTheLeft();
        Rectangle[] rechtsStrömungen = info.getScene().getStreamsToTheRight();


        if (linksStrömungen != null && linksStrömungen.length > 0) {
            for (Rectangle r : linksStrömungen) {
                Rectangle shrunk = new Rectangle(r.x + margin, r.y + margin,
                        Math.max(0, r.width - 2*margin), Math.max(0, r.height - 2*margin));
                if (shrunk.contains((int)meinX, (int)meinY)) {
                    return (float)Math.PI; 
                }
            }
        }

        if (rechtsStrömungen != null && rechtsStrömungen.length > 0) {
            for (Rectangle r : rechtsStrömungen) {
                Rectangle shrunk = new Rectangle(r.x + margin, r.y + margin,
                        Math.max(0, r.width - 2*margin), Math.max(0, r.height - 2*margin));
                if (shrunk.contains((int)meinX, (int)meinY)) {
                    return 0; 
                }
            }
        }

        return Float.NaN; 
    }

    private List<Point> berechnePfad(Point start, Point ziel, Path2D[] hindernisse) {
        List<Point> pfad = new ArrayList<>();
        Queue<Point> queue = new LinkedList<>();
        HashMap<Point, Point> woherKamenWir = new HashMap<>();

        queue.add(start);
        woherKamenWir.put(start, null);

        Point gefundenesZiel = start;

        while (!queue.isEmpty()) {
            Point aktuell = queue.poll();

            if (Abstand(aktuell.x, aktuell.y, ziel.x, ziel.y) < (GRID_SIZE * GRID_SIZE) / 2) {
                gefundenesZiel = Math.abs(aktuell.x - ziel.x) < GRID_SIZE && Math.abs(aktuell.y - ziel.y) < GRID_SIZE ? aktuell : gefundenesZiel;
                gefundenesZiel = aktuell;
                break;
            }

            int[][] richtungen = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
            for (int[] dir : richtungen) {
                Point nachbar = new Point(aktuell.x + dir[0] * GRID_SIZE, aktuell.y + dir[1] * GRID_SIZE);

                if (!woherKamenWir.containsKey(nachbar) && !istBlockiert(nachbar, hindernisse)) {
                    queue.add(nachbar);
                    woherKamenWir.put(nachbar, aktuell);
                }
            }
        }

        Point p = gefundenesZiel;
        while (p != null) {
            pfad.add(p);
            p = woherKamenWir.get(p);
        }

        Collections.reverse(pfad);
        pfad.add(ziel);
        return pfad;
    }

    private boolean istBlockiert(Point p, Path2D[] hindernisse) {
        if (p.x < 0 || p.x > info.getScene().getWidth() || p.y < 0 || p.y > info.getScene().getHeight()) {
            return true;
        }

        for (Path2D h : hindernisse) {
            if (h.contains(p.x, p.y)) return true;
        }
        return false;
    }

    @Override
    public PlayerAction update() {

        float meinX = info.getX();
        float meinY = info.getY();
        float MeinWinkel = info.getOrientation();
        int weltW = info.getScene().getWidth();
        int weltH = info.getScene().getHeight();


        if (info.getScore() > altePunktzahl && !Perlenliste.isEmpty()) {
            Perlenliste.remove(NähestePerle(Perlenliste, meinX, meinY));
            altePunktzahl = info.getScore();
        }

        if (info.getMoney() > altesGeld && !muellListe.isEmpty()) {
            muellListe.remove(NähestePerle(muellListe, meinX, meinY));
            altesGeld = info.getMoney();
        }


        if (meinY <= 5 && !warenWirObenAmOberflächenlevel && letztesZielPunkt != null) {

            boolean wasAPerle = Perlenliste.remove(letztesZielPunkt);
            boolean wasMuell = muellListe.remove(letztesZielPunkt);


            if (wasAPerle) {
                originalPerlenliste.remove(letztesZielPunkt);
            }

            if (wasAPerle || wasMuell) {

                langzeitPfad.clear();
                letztesEndZiel = null;
            }
            warenWirObenAmOberflächenlevel = true;
        } else if (meinY > 10) {
            warenWirObenAmOberflächenlevel = false;
        }

        Point EndZiel = new Point((int)meinX, (int)meinY);

      
        if (!hatUpgrades) {


            if (kaufSchritt == 1) {
                if (info.getMoney() < altesGeld) {
                    altesGeld = info.getMoney();
                    hatUpgrades = true; 
                } else {
                    return new DivingAction(0, 0); 
            }


            if (!hatUpgrades) {
                if (info.getMoney() < 2) {
                    EndZiel = NähestePerle(muellListe, meinX, meinY);
                    if (EndZiel.equals(new Point((int)meinX, (int)meinY))) {
                        EndZiel = NähestePerle(Perlenliste, meinX, meinY);
                    }

                } else {
                    int shopX = info.getScene().getShopPosition();
                    EndZiel = new Point(shopX, 0);

                 
                    if (meinY <= 1 && Math.abs(meinX - shopX) < 1) {
                        altesGeld = info.getMoney(); // Aktuelles Geld vor dem Kauf merken
                        kaufSchritt = 1;
                        return new ShoppingAction(ShoppingItem.BALLOON_SET);
                    }
                }
            }
        }

        if (hatUpgrades) {
            EndZiel = NähestePerle(Perlenliste, meinX, meinY);

           
            if (Perlenliste.isEmpty() && !originalPerlenliste.isEmpty()) {
                EndZiel = NähestePerle(originalPerlenliste, meinX, meinY);
            }
        }

        if (langzeitPfad.isEmpty() || letztesEndZiel == null || !letztesEndZiel.equals(EndZiel)) {
            langzeitPfad = berechnePfad(new Point((int)meinX, (int)meinY), EndZiel, info.getScene().getObstacles());
            letztesEndZiel = EndZiel;
        }

        Point Ziel = langzeitPfad.get(0);

        if (Abstand(Ziel.x, Ziel.y, meinX, meinY) < (GRID_SIZE * GRID_SIZE)/2) {
            if (langzeitPfad.size() > 1) {
                langzeitPfad.remove(0);
                Ziel = langzeitPfad.get(0);
            }
        }

        if (info.getAir() <= info.getMaxAir() * 0.5f) {
            Ziel = new Point((int) meinX, 0);
            langzeitPfad.clear();
            letztesEndZiel = null;
        }

        float ZielWinkel = (float) Math.atan2(Ziel.y - meinY, Ziel.x - meinX);


        if (meinX < 5 || meinX > weltW - 5 && meinY < (weltH)) {
            ZielWinkel = MeinWinkel + (float) Math.PI;
        }


        float fischWinkel = FischAusweichWinkel(meinX, meinY);
        if (!Float.isNaN(fischWinkel)) {
            double zielX = Math.cos(ZielWinkel);
            double zielY = Math.sin(ZielWinkel);

            double fluchtGewicht = 1.5;
            double fluchtX = Math.cos(fischWinkel) * fluchtGewicht;
            double fluchtY = Math.sin(fischWinkel) * fluchtGewicht;

            ZielWinkel = (float) Math.atan2(zielY + fluchtY, zielX + fluchtX);
        }

        float strömungsGegenlenk = StrömungsGegenlenkWinkel(meinX, meinY);
        if (!Float.isNaN(strömungsGegenlenk)) {

            double zielX = Math.cos(ZielWinkel);
            double zielY = Math.sin(ZielWinkel);

            double gegenX = Math.cos(strömungsGegenlenk) * 1.0;
            double gegenY = Math.sin(strömungsGegenlenk) * 1.0;

            ZielWinkel = (float) Math.atan2(zielY + gegenY, zielX + gegenX);
        }

        double distanz = Abstand(Ziel.x, Ziel.y, meinX, meinY);
        float[] fühler = {MeinWinkel, MeinWinkel - (float)(Math.PI/6), MeinWinkel + (float)(Math.PI/6)};
        boolean hindernisVoraus = false;
        Path2D[] hindernis = info.getScene().getObstacles();

        for (int j = 0; j < hindernis.length; j++) {
            for (int k = 0; k < fühler.length; k++) {
                for (int i = 10; i <= 60; i += 10) {
                    if (hindernis[j].contains(meinX + i * Math.cos(fühler[k]), meinY + i * Math.sin(fühler[k])) && (i * i) < distanz) {
                        if (k == 1) {
                            ZielWinkel = MeinWinkel + (float)(Math.PI / 2);
                        } else {
                            ZielWinkel = MeinWinkel - (float)(Math.PI / 2);
                        }
                        hindernisVoraus = true;
                        break;
                    }
                }
                if (hindernisVoraus) break;
            }
            if (hindernisVoraus) break;
        }

        if (meinX < 5 || meinX > weltW - 5 && meinY < (weltH)) {
            ZielWinkel = MeinWinkel + (float) Math.PI;
        }

        float angleDiff = ZielWinkel - MeinWinkel;
        if (angleDiff > Math.PI) {
            angleDiff -= 2 * Math.PI;
        }
        if (angleDiff < -Math.PI) {
            angleDiff += 2 * Math.PI;
        }

        float aktuelleDrehgeschwindigkeit = info.getAngularVelocity();

        float pFaktor = 5.0f;
        float dFaktor = 2.0f;

        float lenkung = (angleDiff * pFaktor) - (aktuelleDrehgeschwindigkeit * dFaktor);


        letztesZielPunkt = new Point(EndZiel.x, EndZiel.y);

        return new DivingAction(info.getMaxAcceleration(), lenkung);
    }
}