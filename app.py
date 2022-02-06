import os

from flask import Flask, url_for
from flask_restful import Resource, Api
from flask_cors import cross_origin, CORS
from markupsafe import escape

app = Flask(__name__)
api = Api(app)
CORS(app)

@app.route("/<name>")
def hello(name):
    return f"hello, {escape(name)}"

class TestImages(Resource):
    @cross_origin()
    def get(self):
        images = os.listdir(os.path.join("static", "testimages"))
        return {"images":[url_for("static", filename="testimages/" + image) for image in images]}

api.add_resource(TestImages, "/testimages")

if __name__ == "__main__":
    app.run(host="localhost", port=8080, debug=True)