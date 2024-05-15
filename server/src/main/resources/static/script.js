document.getElementById("target").value = "http://example.org/A";
document.getElementById("data").value =
    `@base <http://example.org/> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
<http://example.com/ontology> a owl:Ontology ;
    owl:imports <https://w3id.org/CEON/ontology/actor/0.2/> .
<A> a <Vowel> .
<Vowel> rdfs:subClassOf <Letter> .`;


document.getElementById("myform").onsubmit = (event) => {
    event.preventDefault();
    process();
};


console.log("before process")
function process(e) {
    console.log("do process")
    let target = document.getElementById("target").value;
    let data = document.getElementById("data").value;

    // Define the URL and parameters
    const url = '/api/types';
    const params = { target, data };

    // Construct the URL with parameters
    const queryParams = new URLSearchParams(params);
    const urlWithParams = `${url}?${queryParams}`;

    // Perform the GET request
    fetch(urlWithParams)
        .then(response => {
            if (!response.ok) {
                throw new Error(response.statusText);
            }
            return response.json();
        })
        .then(data => {
            document.getElementById("result").value = data;
        })
        .catch(error => {
            console.error('Error:', error);
        });



}