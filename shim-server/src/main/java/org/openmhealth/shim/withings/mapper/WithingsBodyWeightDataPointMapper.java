package org.openmhealth.shim.withings.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.openmhealth.schema.domain.omh.BodyWeight;
import org.openmhealth.schema.domain.omh.DataPoint;
import org.openmhealth.schema.domain.omh.MassUnit;
import org.openmhealth.schema.domain.omh.MassUnitValue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.openmhealth.shim.common.mapper.JsonNodeMappingSupport.*;
import static org.openmhealth.shim.withings.mapper.WithingsBodyMeasureDataPointMapper.BodyMeasureTypes.WEIGHT;


/**
 * A mapper from Withings Body Measure endpoint responses (/measure?action=getmeas) to {@link BodyWeight} objects when a
 * body weight value is present in the body measure group
 *
 * @author Chris Schaefbauer
 * @see <a href="http://oauth.withings.com/api/doc#api-Measure-get_measure">Body Measures API documentation</a>
 */
public class WithingsBodyWeightDataPointMapper extends WithingsBodyMeasureDataPointMapper<BodyWeight> {

    /**
     * Maps a JSON response node from the Withings body measure endpoint into a {@link BodyWeight} measure
     *
     * @param node list node from the array "measuregrp" contained in the "body" of the endpoint response
     * @param timeZoneFullName a string containing the full name of the time zone (e.g., America/Los_Angeles) from the
     * "timezone" property of the "body" of the body measure endpoint response
     * @return a {@link DataPoint} object containing a {@link BodyWeight} measure with the appropriate values from
     * the JSON node parameter, wrapped as an {@link Optional}
     */
    @Override
    Optional<DataPoint<BodyWeight>> asDataPoint(JsonNode node, String timeZoneFullName) {
        JsonNode measuresNode = asRequiredNode(node, "measures");
        if(isGoal(node)){
            return Optional.empty();
        }
        Double value = null;
        Long unit = null;
        for (JsonNode measureNode : measuresNode) {
            if (asRequiredLong(measureNode, "type") == WEIGHT.getIntVal()) {
                value = asRequiredDouble(measureNode, "value");
                unit = asRequiredLong(measureNode, "unit");
            }
        }

        if (value == null || unit == null) {
            return Optional.empty(); // there was no body weight measure in this node
        }

        BodyWeight.Builder bodyWeightBuilder = new BodyWeight.Builder(new MassUnitValue(MassUnit.KILOGRAM,
                actualValueOf(value, unit)));

        Optional<Long> dateTimeInUtcSec = asOptionalLong(node, "date");
        if (dateTimeInUtcSec.isPresent()) {
            OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(Instant.ofEpochSecond(dateTimeInUtcSec.get()),
                    ZoneId.of(timeZoneFullName));

            bodyWeightBuilder.setEffectiveTimeFrame(offsetDateTime);
        }

        Optional<String> userComment = asOptionalString(node, "comment");

        if (userComment.isPresent()) {
            bodyWeightBuilder.setUserNotes(userComment.get());
        }

        Optional<Long> externalId = asOptionalLong(node, "grpid");

        BodyWeight bodyWeight = bodyWeightBuilder.build();

        return Optional.of(newDataPoint(bodyWeight, RESOURCE_API_SOURCE_NAME, externalId.orElse(null),
                isSensed(node).orElse(null), null));
    }


}
