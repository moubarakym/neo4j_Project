package com.sorbonne;

import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;

public class UpdatingWeight {

    @Context
    public GraphDatabaseService db;

    @Procedure(name = "mypackage.UpdatingWeight", mode = Mode.WRITE)
    @Description("Met à jour les poids et les biais d'un réseau de neurones en utilisant Adam.")
    public Stream<UpdateResult> UpdatingWeight(
            @Name("learning_rate") double learningRate,
            @Name("beta1") double beta1,
            @Name("beta2") double beta2,
            @Name("epsilon") double epsilon,
            @Name("t") double t) {

        try(Transaction tx = db.beginTx()){

            tx.execute("MATCH (output:Neuron {type: 'output'})<-[r:CONNECTED_TO]-(prev:Neuron)\n"+
            "MATCH (output)-[outputsValues_R:CONTAINS]->(row_for_outputs:Row {type: 'outputsRow'})\n" +
            "WITH DISTINCT output, r, prev, outputsValues_R, row_for_outputs,\n" +
                    "CASE\n" +
            "WHEN output.activation_function = 'softmax' THEN outputsValues_R.output - outputsValues_R.expected_output \n" +
            "WHEN output.activation_function = 'sigmoid' THEN (outputsValues_R.output - outputsValues_R.expected_output) * outputsValues_R.output * (1 - outputsValues_R.output)\n" +
            "WHEN output.activation_function = 'tanh' THEN (outputsValues_R.output - outputsValues_R.expected_output) * (1 - outputsValues_R.output^2)\n" +
            "ELSE outputsValues_R.output - outputsValues_R.expected_output\n" + // Linear activation
            "END AS gradient\n" +
            "MATCH (prev)-[r:CONNECTED_TO]->(output)\n" +
                    "SET r.m ='" + beta1 +"' * COALESCE(r.m, 0) + (1 - '" + beta1+ "') * gradient * COALESCE(prev.output, 0)\n"+
            "SET r.v = '" + beta2 +"' * COALESCE(r.v, 0) + (1 - '" + beta2 +"') * (gradient * COALESCE(prev.output, 0))^2\n"+
            "SET r.weight = r.weight - '"+learningRate+"' * (r.m / (1 - ('" + beta1 + "' ^ '" + t + "'))) / \n" +
                    "(SQRT(r.v / (1 - ('" + beta2 + "' ^'" + t + "'))) + '" +epsilon+ "')"+
            "SET output.m_bias = '"+ beta1 +"' * COALESCE(output.m_bias, 0) + (1 - '"+ beta1 + "') * gradient\n" +
            "SET output.v_bias = '"+ beta2 + "' * COALESCE(output.v_bias, 0) + (1 - '" +beta2+"') * (gradient^2)\n" +
            "SET output.bias = output.bias -'" + learningRate +"' * (output.m_bias / (1 - ( '" + beta1+ "' ^ '" + t +"'))) / \n" +
                    "(SQRT(output.v_bias / (1 - ( '" + beta2 + "' ^ '"+t+"'))) + '" + epsilon+"' ) \n" +
            "SET output.gradient = gradient\n" +
            "RETURN COUNT(output) AS updatedNeurons");
            tx.commit();
            return Stream.of(new UpdateResult(1));
        } catch (Exception e) {
            return Stream.of(new UpdateResult(0));
        }

        github_pat_11A6DIM6I 0 m 9DgzzIQRXGZ_2n3plvbc21 O Er8YPbRPtcF5jKZEQL9gTyqnzAP7B32KYPPUMXRJbIqolwPA 0 O
    }

    public static class UpdateResult {
        public long updatedNeurons;

        public UpdateResult(long updatedNeurons) {
            this.updatedNeurons = updatedNeurons;
        }
    }
}

