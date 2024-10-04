package com.example.paint1_1;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
public class PaintShapes {

    public abstract static class Shape extends PaintController{
        protected double startX, startY, endX, endY;
        protected Color color;
        protected double lineWidth;
        public Shape(double startX, double startY, double endX, double endY, Color color, double lineWidth) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.color = color;
            this.lineWidth = lineWidth;}
        // Abstract method to draw the shape
        public abstract void draw(GraphicsContext gc);}

    public static class LineShape extends Shape {
        public LineShape(double startX, double startY, double endX, double endY, Color color, double lineWidth) {
            super(startX, startY, endX, endY, color, lineWidth);}
        @Override public void draw(GraphicsContext gc) {
            gc.setStroke(color);
            gc.setLineWidth(lineWidth);
            gc.strokeLine(startX, startY, endX, endY);}}

    public static class SquareShape extends Shape {
        public SquareShape(double startX, double startY, double endX, double endY, Color color, double lineWidth) {
            super(startX, startY, endX, endY, color, lineWidth);}
        @Override public void draw(GraphicsContext gc) {
            double sideLength = Math.min(Math.abs(endX - startX), Math.abs(endY - startY));
            double x = Math.min(startX, endX);
            double y = Math.min(startY, endY);
            gc.setStroke(color);
            gc.setLineWidth(lineWidth);
            gc.strokeRect(x, y, sideLength, sideLength);}}

    public static class RectangleShape extends Shape {
        public RectangleShape(double startX, double startY, double endX, double endY, Color color, double lineWidth) {
            super(startX, startY, endX, endY, color, lineWidth);}
        @Override public void draw(GraphicsContext gc) {
            gc.setStroke(color);
            gc.setLineWidth(lineWidth);
            gc.strokeRect(Math.min(startX, endX), Math.min(startY, endY), Math.abs(endX - startX), Math.abs(endY - startY));}}

    public static class DiamondShape extends Shape {
        public DiamondShape(double startX, double startY, double endX, double endY, Color color, double lineWidth) {
            super(startX, startY, endX, endY, color, lineWidth);}
        @Override public void draw(GraphicsContext gc) {
            double centerX = (startX + endX) / 2;
            double centerY = (startY + endY) / 2;
            double halfWidth = Math.abs(endX - startX) / 2;
            double halfHeight = Math.abs(endY - startY) / 2;
            double[] xPoints = {centerX, centerX - halfWidth, centerX, centerX + halfWidth};
            double[] yPoints = {centerY - halfHeight, centerY, centerY + halfHeight, centerY};
            gc.setStroke(color);
            gc.setLineWidth(lineWidth);
            gc.strokePolygon(xPoints, yPoints, 4);}}

    public static class TriangleShape extends Shape {
        public TriangleShape(double startX, double startY, double endX, double endY, Color color, double lineWidth) {
            super(startX, startY, endX, endY, color, lineWidth);}
        @Override public void draw(GraphicsContext gc) {
            double[] xPoints = {startX, endX, (startX + endX) / 2};
            double[] yPoints = {endY, endY, startY};
            gc.setStroke(color);
            gc.setLineWidth(lineWidth);
            gc.strokePolygon(xPoints, yPoints, 3);}}

    public static class OvalShape extends Shape {
        public OvalShape(double startX, double startY, double endX, double endY, Color color, double lineWidth) {
            super(startX, startY, endX, endY, color, lineWidth);}
        @Override public void draw(GraphicsContext gc) {
            double width = Math.abs(endX - startX);
            double height = Math.abs(endY - startY);
            double x = Math.min(startX, endX);
            double y = Math.min(startY, endY);
            gc.setStroke(color);
            gc.setLineWidth(lineWidth);
            gc.strokeOval(x, y, width, height);}}

    public static class CircleShape extends Shape {
        public CircleShape(double startX, double startY, double endX, double endY, Color color, double lineWidth) {
            super(startX, startY, endX, endY, color, lineWidth);}
        @Override public void draw(GraphicsContext gc) {
            double radius = Math.hypot(endX - startX, endY - startY);
            gc.setStroke(color);
            gc.setLineWidth(lineWidth);
            gc.strokeOval(startX - radius, startY - radius, radius * 2, radius * 2);}}


}
