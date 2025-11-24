package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * @param views
     *            the views to attach
     */
    public DrawNumberApp(final String configPath, final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        final Configuration.Builder configBuilder = new Configuration.Builder();
        final Map<String, Consumer<Integer>> configLoader = Map.of(
            "maximum", configBuilder::setMax,
            "minimum", configBuilder::setMin,
            "attempts", configBuilder::setAttempts
        );
        Configuration config = readConfiguration(configPath, configLoader, configBuilder);
        if (!config.isConsistent()) {
            showError("inconsistent config, loaded default config");
            config = new Configuration.Builder().build(); // default config
        }
        model = new DrawNumberImpl(config.getMin(), config.getMax(), config.getAttempts());
    }

    /**
     * reads the configuration from the file system and returns a configuration.
     * 
     * @param path config path
     * @param loader a map matching each property of the config to the it should be set
     * @param builder the configuration builder itself
     * @return a configuration. it is NOT required to be valid,
     * it is up to the caller to handle an inconsisten config (not load game or load a default config)
     */
    private Configuration readConfiguration(
            String path,
            Map<String, Consumer<Integer>> loader,
            Configuration.Builder builder) { 
        
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(ClassLoader.getSystemResourceAsStream(path),
            StandardCharsets.UTF_8
        ))) {
            br.lines()
                .filter(line -> !line.isBlank())
                .forEach(line -> {
                    final String[] splitLine = line.split(":");
                    final String option = splitLine[0].trim();
                    final int value = Integer.parseInt(splitLine[1].trim());

                    final Consumer<Integer> handler = loader.get(option);
                    if (handler != null) {
                        handler.accept(value);
                    } else {
                        showError("unknown option provided");
                    }
                });
        } catch (final IOException | NumberFormatException | NullPointerException ex) {
            showError(ex.getMessage());
        }
        return builder.build();
    }

    /**
     * makes the view(s) display the error.
     * 
     * @param err the error message to display
     */
    private void showError(final String err) {
        views.forEach(v -> v.displayError(err));
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *            ignored
     * @throws FileNotFoundException 
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp("config.yml", new DrawNumberViewImpl());
    }

}
