package com.ronalonis.illagerblabber.voice;

public abstract class IllagerState {
    private IllagerState() {
    }

    public static final class Passive extends IllagerState {
        public static final Passive INSTANCE = new Passive();

        private Passive() {
        }
    }

    public static final class Spotted extends IllagerState {
        public static final Spotted INSTANCE = new Spotted();

        private Spotted() {
        }
    }

    public static final class Combat extends IllagerState {
        public static final Combat INSTANCE = new Combat();

        private Combat() {
        }
    }

    public static final class Hurt extends IllagerState {
        public static final Hurt INSTANCE = new Hurt();

        private Hurt() {
        }
    }

    public static final class Victory extends IllagerState {
        public static final Victory INSTANCE = new Victory();

        private Victory() {
        }
    }
}
