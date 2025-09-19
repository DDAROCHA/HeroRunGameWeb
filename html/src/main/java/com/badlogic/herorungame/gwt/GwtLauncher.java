package com.badlogic.herorungame.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.herorungame.Main;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
        @Override
        public GwtApplicationConfiguration getConfig () {

            // Fijo 800x480
            GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(800, 480);

            // Opcional: si el contenedor tiene padding, se puede ajustar
            cfg.padHorizontal = 0;
            cfg.padVertical = 0;

            return cfg;

            // Resizable application, uses available space in browser with no padding:
            //GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(true);
            //cfg.padVertical = 0;
            //cfg.padHorizontal = 0;
            //return cfg;
            // If you want a fixed size application, comment out the above resizable section,
            // and uncomment below:
            //return new GwtApplicationConfiguration(800, 480);
        }

        @Override
        public ApplicationListener createApplicationListener () {
            return new Main();
        }
}
