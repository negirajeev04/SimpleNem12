package simplenem12;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SimpleNem12ParserImpl implements SimpleNem12Parser {

	private static final Logger LOGGER = Logger.getLogger("SimpleNem12ParserImpl");

	@Override
	public Collection<MeterRead> parseSimpleNem12(File simpleNem12File) {

		final List<MeterRead> meterreads = new ArrayList<>();

		Path path = Paths.get(simpleNem12File.toURI());

		Supplier<Stream<String>> streamSupplier = () -> getStreamFromFile(path).get();

		if (!streamSupplier.get().findFirst().get().equals("100")) { // First line

			LOGGER.log(Level.SEVERE, "Invalid input file. RecordType 100 must be the first line in the file.");
			return Collections.<MeterRead>emptyList();

		} else if (!streamSupplier.get().reduce((l1, l2) -> l2).get().equals("900")) { // last line

			LOGGER.log(Level.SEVERE, "Invalid input file. RecordType 900 must be the last line in the file");
			return Collections.<MeterRead>emptyList();

		}

		/*
		 * Format the csv stream to add NemId at the end of each 300 record
		 */
		final String[] lastNemId = new String[1];
		Stream<String> formattedStream = streamSupplier.get().peek(str -> {
			if (str.startsWith("200")) {
				lastNemId[0] = str.split(",")[1];
			}
		}).map(str -> str.startsWith("300") ? str+","+lastNemId[0] : str);
		
		/*
		 * Create MeterRead for each 200 and MeterVolumes for each 300
		 */
		formattedStream.forEach((str) -> {
			
			if (str.startsWith("200")) {
				String[] splits200 = str.split(",");
				MeterRead meterRead = new MeterRead(splits200[1], EnergyUnit.valueOf(splits200[2]));
				meterreads.add(meterRead);

			} else if (str.startsWith("300")) {

				String[] splits300 = str.split(",");
				meterreads.get(meterreads.size()-1).getVolumes().put(LocalDate.parse(splits300[1], DateTimeFormatter.BASIC_ISO_DATE),
						new MeterVolume(BigDecimal.valueOf(Double.parseDouble(splits300[2])),
								Quality.valueOf(splits300[3])));

			}

		});

		return meterreads;
	}

	private Optional<Stream<String>> getStreamFromFile(Path path) {

		Optional<Stream<String>> stream = Optional.ofNullable(null);
		try {
			stream = Optional.ofNullable(Files.lines(path));
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage());
		}
		return stream;
	}

}
