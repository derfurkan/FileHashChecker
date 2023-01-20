package de.furkan.filehashchecker;

import java.io.File;

public record Config(File inputFolder, File checkFile, Algorithm algorithm, boolean multiThreading, int threads) {

}
